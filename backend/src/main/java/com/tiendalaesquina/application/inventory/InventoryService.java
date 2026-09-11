package com.tiendalaesquina.application.inventory;

import com.tiendalaesquina.domain.model.AuditEvent;
import com.tiendalaesquina.domain.model.InventoryAdjustment;
import com.tiendalaesquina.domain.model.InventoryMovement;
import com.tiendalaesquina.domain.model.InventoryMovementType;
import com.tiendalaesquina.domain.model.InventorySourceType;
import com.tiendalaesquina.domain.model.Product;
import com.tiendalaesquina.domain.model.StockNotification;
import com.tiendalaesquina.domain.model.StockNotificationType;
import com.tiendalaesquina.domain.model.StockStatus;
import com.tiendalaesquina.domain.model.UserAccount;
import com.tiendalaesquina.domain.repository.AuditEventRepository;
import com.tiendalaesquina.domain.repository.InventoryAdjustmentRepository;
import com.tiendalaesquina.domain.repository.InventoryMovementRepository;
import com.tiendalaesquina.domain.repository.ProductRepository;
import com.tiendalaesquina.domain.repository.StockNotificationRepository;
import com.tiendalaesquina.domain.repository.UserAccountRepository;
import com.tiendalaesquina.exception.ApiException;
import com.tiendalaesquina.web.dto.InventoryAdjustmentRequest;
import com.tiendalaesquina.web.dto.InventoryAdjustmentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Service
public class InventoryService {

    private final ProductRepository products;
    private final UserAccountRepository users;
    private final InventoryAdjustmentRepository adjustments;
    private final InventoryMovementRepository movements;
    private final StockNotificationRepository notifications;
    private final AuditEventRepository auditEvents;

    public InventoryService(ProductRepository products,
                            UserAccountRepository users,
                            InventoryAdjustmentRepository adjustments,
                            InventoryMovementRepository movements,
                            StockNotificationRepository notifications,
                            AuditEventRepository auditEvents) {
        this.products = products;
        this.users = users;
        this.adjustments = adjustments;
        this.movements = movements;
        this.notifications = notifications;
        this.auditEvents = auditEvents;
    }

    @Transactional
    public InventoryAdjustmentResponse adjustStock(InventoryAdjustmentRequest request, String actorEmail) {
        UserAccount actor = users.findByEmail(actorEmail)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "account_not_found",
                        "Cuenta no encontrada",
                        "No fue posible identificar al usuario autenticado"
                ));

        Product product = products.findByIdForUpdate(request.productId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "product_not_found",
                        "Producto no encontrado",
                        "El producto solicitado no existe"
                ));

        if (!product.isActive()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "product_inactive",
                    "Producto inactivo",
                    "No se puede ajustar el inventario de un producto inactivo"
            );
        }

        BigDecimal newStock = request.newStock();
        if (newStock == null || newStock.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "negative_stock_not_allowed",
                    "Stock inválido",
                    "El stock no puede ser negativo"
            );
        }

        if (!product.getUnit().isAllowsDecimal() && newStock.stripTrailingZeros().scale() > 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "decimals_not_allowed",
                    "Unidad no permite decimales",
                    "La unidad de medida " + product.getUnit().getName() + " no permite valores decimales"
            );
        }

        BigDecimal quantityBefore = product.getCurrentStock();
        BigDecimal quantityDelta = newStock.subtract(quantityBefore);

        if (quantityDelta.compareTo(BigDecimal.ZERO) == 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "no_stock_change",
                    "Sin cambio en existencias",
                    "El nuevo stock no puede ser igual a la existencia actual"
            );
        }

        String reason = request.reason().trim();
        Instant now = Instant.now();

        // 1. Actualizar stock del producto
        product.setCurrentStock(newStock, actor);
        products.save(product);

        // 2. Registrar ajuste
        InventoryAdjustment adjustment = new InventoryAdjustment(
                product,
                actor,
                now,
                quantityBefore,
                newStock,
                quantityDelta,
                reason
        );
        adjustment = adjustments.save(adjustment);

        // 3. Registrar movimiento
        InventoryMovement movement = new InventoryMovement(
                product,
                InventoryMovementType.ADJUSTMENT,
                quantityDelta,
                quantityBefore,
                newStock,
                InventorySourceType.ADJUSTMENT,
                adjustment.getId(),
                reason,
                actor,
                now
        );
        movements.save(movement);

        // 4. Evaluar alertas/notificaciones (sin resolver automáticamente)
        handleStockAlerts(product, newStock);

        // 5. Registrar evento de auditoría
        String summary = String.format(
                "Ajuste de inventario en producto %s (%s): stock anterior %s, nuevo stock %s",
                product.getName(), product.getCode(), quantityBefore, newStock
        );
        if (summary.length() > 255) {
            summary = summary.substring(0, 252) + "...";
        }

        Map<String, Object> beforeData = Map.of(
                "currentStock", quantityBefore,
                "stockStatus", StockStatus.from(quantityBefore, product.getMinimumStock()).name()
        );
        Map<String, Object> afterData = Map.of(
                "currentStock", newStock,
                "delta", quantityDelta,
                "reason", reason,
                "stockStatus", product.getStockStatus().name(),
                "adjustmentId", adjustment.getId()
        );

        AuditEvent auditEvent = new AuditEvent(
                actor,
                "INVENTORY_ADJUSTMENT",
                "PRODUCT",
                String.valueOf(product.getId()),
                summary,
                beforeData,
                afterData,
                null,
                null,
                now
        );
        auditEvents.save(auditEvent);

        return new InventoryAdjustmentResponse(
                adjustment.getId(),
                product.getId(),
                product.getCode(),
                product.getName(),
                quantityBefore,
                newStock,
                quantityDelta,
                reason,
                actor.getEmail(),
                adjustment.getAdjustmentDate(),
                adjustment.getCreatedAt(),
                product.getStockStatus().name()
        );
    }

    private void handleStockAlerts(Product product, BigDecimal currentStock) {
        StockStatus status = StockStatus.from(currentStock, product.getMinimumStock());
        if (status == StockStatus.OUT_OF_STOCK) {
            if (!notifications.existsByProductIdAndNotificationTypeAndIsReadFalse(
                    product.getId(), StockNotificationType.OUT_OF_STOCK)) {
                notifications.save(new StockNotification(
                        product,
                        StockNotificationType.OUT_OF_STOCK,
                        "Producto agotado",
                        "El producto " + product.getName() + " se encuentra agotado."
                ));
            }
        } else if (status == StockStatus.LOW_STOCK) {
            if (!notifications.existsByProductIdAndNotificationTypeAndIsReadFalse(
                    product.getId(), StockNotificationType.LOW_STOCK)) {
                notifications.save(new StockNotification(
                        product,
                        StockNotificationType.LOW_STOCK,
                        "Existencia baja",
                        "El producto " + product.getName() + " tiene " + currentStock
                                + " unidades y su mínimo es " + product.getMinimumStock() + "."
                ));
            }
        }
    }
}
