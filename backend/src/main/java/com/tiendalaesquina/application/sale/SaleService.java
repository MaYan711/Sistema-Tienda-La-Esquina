package com.tiendalaesquina.application.sale;

import com.tiendalaesquina.domain.model.AuditEvent;
import com.tiendalaesquina.domain.model.InventoryMovement;
import com.tiendalaesquina.domain.model.InventoryMovementType;
import com.tiendalaesquina.domain.model.InventorySourceType;
import com.tiendalaesquina.domain.model.Product;
import com.tiendalaesquina.domain.model.Sale;
import com.tiendalaesquina.domain.model.SaleItem;
import com.tiendalaesquina.domain.model.StockNotification;
import com.tiendalaesquina.domain.model.StockNotificationType;
import com.tiendalaesquina.domain.model.StockStatus;
import com.tiendalaesquina.domain.model.UserAccount;
import com.tiendalaesquina.domain.repository.AuditEventRepository;
import com.tiendalaesquina.domain.repository.InventoryMovementRepository;
import com.tiendalaesquina.domain.repository.ProductRepository;
import com.tiendalaesquina.domain.repository.SaleItemRepository;
import com.tiendalaesquina.domain.repository.SaleRepository;
import com.tiendalaesquina.domain.repository.StockNotificationRepository;
import com.tiendalaesquina.domain.repository.UserAccountRepository;
import com.tiendalaesquina.exception.ApiException;
import com.tiendalaesquina.web.dto.CreateSaleItemRequest;
import com.tiendalaesquina.web.dto.CreateSaleRequest;
import com.tiendalaesquina.web.dto.SaleItemResponse;
import com.tiendalaesquina.web.dto.SaleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SaleService {

    private static final ZoneId GUATEMALA_ZONE = ZoneId.of("America/Guatemala");

    private final SaleRepository sales;
    private final SaleItemRepository saleItems;
    private final ProductRepository products;
    private final UserAccountRepository users;
    private final InventoryMovementRepository movements;
    private final StockNotificationRepository notifications;
    private final AuditEventRepository auditEvents;

    public SaleService(
            SaleRepository sales,
            SaleItemRepository saleItems,
            ProductRepository products,
            UserAccountRepository users,
            InventoryMovementRepository movements,
            StockNotificationRepository notifications,
            AuditEventRepository auditEvents
    ) {
        this.sales = sales;
        this.saleItems = saleItems;
        this.products = products;
        this.users = users;
        this.movements = movements;
        this.notifications = notifications;
        this.auditEvents = auditEvents;
    }

    @Transactional
    public SaleResponse create(CreateSaleRequest request, String actorEmail) {
        UserAccount actor = requireActor(actorEmail);

        validateNoDuplicateProducts(request.items());

        List<ProductSaleData> saleData = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CreateSaleItemRequest itemRequest : request.items()) {
            Product product = requireActiveProductForUpdate(itemRequest.productId());

            validateQuantity(product, itemRequest.quantity());

            BigDecimal quantity = itemRequest.quantity()
                    .setScale(3, RoundingMode.UNNECESSARY);

            if (product.getCurrentStock().compareTo(quantity) < 0) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "insufficient_stock",
                        "Existencia insuficiente",
                        "No hay suficiente existencia disponible para el producto " + product.getName()
                );
            }

            BigDecimal unitPrice = product.getSalePrice()
                    .setScale(2, RoundingMode.UNNECESSARY);

            BigDecimal unitCost = product.getPurchasePrice()
                    .setScale(4, RoundingMode.UNNECESSARY);

            BigDecimal lineTotal = quantity
                    .multiply(unitPrice)
                    .setScale(2, RoundingMode.HALF_UP);

            saleData.add(new ProductSaleData(
                    product,
                    quantity,
                    unitPrice,
                    unitCost,
                    lineTotal
            ));

            subtotal = subtotal.add(lineTotal);
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal;

        BigDecimal cashReceived = request.cashReceived()
                .setScale(2, RoundingMode.UNNECESSARY);

        if (cashReceived.compareTo(total) < 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "insufficient_cash",
                    "Efectivo insuficiente",
                    "El efectivo recibido debe ser igual o mayor que el total de la venta"
            );
        }

        BigDecimal change = cashReceived
                .subtract(total)
                .setScale(2, RoundingMode.HALF_UP);

        String saleNumber = generateSaleNumber();

        Sale sale = new Sale(
                saleNumber,
                actor,
                subtotal,
                total,
                cashReceived,
                change,
                normalizeNullable(request.notes())
        );

        sale = sales.save(sale);

        List<SaleItemResponse> itemResponses = new ArrayList<>();

        for (ProductSaleData data : saleData) {
            Product product = data.product();

            BigDecimal quantityBefore = product.getCurrentStock();

            BigDecimal quantityAfter = quantityBefore
                    .subtract(data.quantity())
                    .setScale(3, RoundingMode.UNNECESSARY);

            product.setCurrentStock(quantityAfter, actor);
            products.save(product);

            SaleItem item = new SaleItem(
                    sale,
                    product,
                    data.quantity(),
                    data.unitPrice(),
                    data.unitCostSnapshot(),
                    data.lineTotal()
            );

            item = saleItems.save(item);

            sale.addItem(item);

            movements.save(new InventoryMovement(
                    product,
                    InventoryMovementType.SALE,
                    data.quantity().negate(),
                    quantityBefore,
                    quantityAfter,
                    InventorySourceType.SALE,
                    sale.getId(),
                    "Venta " + sale.getSaleNumber(),
                    actor,
                    Instant.now()
            ));

            createStockNotificationIfNeeded(product);

            itemResponses.add(toItemResponse(item));
        }

        auditEvents.save(new AuditEvent(
                actor,
                "SALE_CREATED",
                "SALE",
                String.valueOf(sale.getId()),
                "Venta registrada: " + sale.getSaleNumber(),
                null,
                Map.of(
                        "saleNumber", sale.getSaleNumber(),
                        "items", itemResponses.size(),
                        "totalAmount", total,
                        "cashReceived", cashReceived,
                        "changeAmount", change
                ),
                null,
                null,
                Instant.now()
        ));

        return toResponse(sale, itemResponses);
    }

    @Transactional(readOnly = true)
    public SaleResponse get(Long id) {
        Sale sale = requireSale(id);

        List<SaleItem> items = saleItems.findBySaleId(id);

        return toResponse(
                sale,
                items.stream().map(this::toItemResponse).toList()
        );
    }

    @Transactional(readOnly = true)
    public Page<SaleResponse> search(
            LocalDate fromDate,
            LocalDate toDate,
            String reference,
            Pageable pageable
    ) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_date_range",
                    "Rango de fechas invalido",
                    "La fecha inicial no puede ser posterior a la fecha final"
            );
        }

        Specification<Sale> specification =
                (root, query, cb) -> cb.conjunction();

        if (fromDate != null) {
            Instant from = fromDate
                    .atStartOfDay(GUATEMALA_ZONE)
                    .toInstant();

            specification = specification.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("saleDate"), from)
            );
        }

        if (toDate != null) {
            Instant untilExclusive = toDate
                    .plusDays(1)
                    .atStartOfDay(GUATEMALA_ZONE)
                    .toInstant();

            specification = specification.and((root, query, cb) ->
                    cb.lessThan(root.get("saleDate"), untilExclusive)
            );
        }

        String normalizedReference = normalizeNullable(reference);

        if (normalizedReference != null) {
            String pattern =
                    "%" + normalizedReference.toLowerCase(Locale.ROOT) + "%";

            specification = specification.and((root, query, cb) ->
                    cb.like(
                            cb.lower(root.get("saleNumber")),
                            pattern
                    )
            );
        }

        return sales.findAll(specification, pageable)
                .map(sale -> {
                    List<SaleItem> items = saleItems.findBySaleId(sale.getId());

                    return toResponse(
                            sale,
                            items.stream().map(this::toItemResponse).toList()
                    );
                });
    }

    private UserAccount requireActor(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "account_not_found",
                        "Cuenta no encontrada",
                        "No fue posible identificar al usuario autenticado"
                ));
    }

    private Product requireActiveProductForUpdate(Long id) {
        Product product = products.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "product_not_found",
                        "Producto no encontrado",
                        "Uno de los productos seleccionados no existe"
                ));

        if (!product.isActive()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "product_inactive",
                    "Producto inactivo",
                    "No se puede vender un producto inactivo"
            );
        }

        return product;
    }

    private Sale requireSale(Long id) {
        return sales.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "sale_not_found",
                        "Venta no encontrada",
                        "La venta solicitada no existe"
                ));
    }

    private void validateNoDuplicateProducts(List<CreateSaleItemRequest> items) {
        Set<Long> productIds = new HashSet<>();

        for (CreateSaleItemRequest item : items) {
            if (!productIds.add(item.productId())) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "duplicate_product_in_sale",
                        "Producto duplicado",
                        "Un producto no puede aparecer mas de una vez en la misma venta"
                );
            }
        }
    }

    private void validateQuantity(Product product, BigDecimal quantity) {
        if (!product.getUnit().isAllowsDecimal()
                && quantity.stripTrailingZeros().scale() > 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "decimal_quantity_not_allowed",
                    "Cantidad invalida",
                    "La unidad de medida del producto "
                            + product.getName()
                            + " no permite cantidades decimales"
            );
        }
    }

    private void createStockNotificationIfNeeded(Product product) {
        StockStatus status = product.getStockStatus();

        if (status == StockStatus.OUT_OF_STOCK) {
            if (!notifications.existsByProductIdAndNotificationTypeAndIsReadFalse(
                    product.getId(),
                    StockNotificationType.OUT_OF_STOCK
            )) {
                notifications.save(new StockNotification(
                        product,
                        StockNotificationType.OUT_OF_STOCK,
                        "Producto agotado",
                        "El producto " + product.getName() + " se encuentra agotado."
                ));
            }
        } else if (status == StockStatus.LOW_STOCK) {
            if (!notifications.existsByProductIdAndNotificationTypeAndIsReadFalse(
                    product.getId(),
                    StockNotificationType.LOW_STOCK
            )) {
                notifications.save(new StockNotification(
                        product,
                        StockNotificationType.LOW_STOCK,
                        "Existencia baja",
                        "El producto "
                                + product.getName()
                                + " tiene "
                                + product.getCurrentStock()
                                + " unidades y su minimo es "
                                + product.getMinimumStock()
                                + "."
                ));
            }
        }
    }

    private SaleItemResponse toItemResponse(SaleItem item) {
        Product product = item.getProduct();

        return new SaleItemResponse(
                item.getId(),
                product.getId(),
                product.getCode(),
                product.getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getUnitCostSnapshot(),
                item.getLineTotal()
        );
    }

    private SaleResponse toResponse(
            Sale sale,
            List<SaleItemResponse> items
    ) {
        UserAccount actor = sale.getSoldBy();

        return new SaleResponse(
                sale.getId(),
                sale.getSaleNumber(),
                actor.getId(),
                actor.getEmail(),
                sale.getSaleDate(),
                sale.getSubtotalAmount(),
                sale.getTotalAmount(),
                sale.getCashReceived(),
                sale.getChangeAmount(),
                sale.getStatus().name(),
                sale.getNotes(),
                items,
                sale.getCreatedAt()
        );
    }

    private String generateSaleNumber() {
        String suffix = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase(Locale.ROOT);

        return "VTA-" + suffix;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private record ProductSaleData(
            Product product,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal unitCostSnapshot,
            BigDecimal lineTotal
    ) {
    }
}