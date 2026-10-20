package com.tiendalaesquina.application.stockentry;

import com.tiendalaesquina.domain.model.AuditEvent;
import com.tiendalaesquina.domain.model.InventoryMovement;
import com.tiendalaesquina.domain.model.InventoryMovementType;
import com.tiendalaesquina.domain.model.InventorySourceType;
import com.tiendalaesquina.domain.model.Product;
import com.tiendalaesquina.domain.model.StockEntry;
import com.tiendalaesquina.domain.model.StockEntryItem;
import com.tiendalaesquina.domain.model.Supplier;
import com.tiendalaesquina.domain.model.UserAccount;
import com.tiendalaesquina.domain.repository.AuditEventRepository;
import com.tiendalaesquina.domain.repository.InventoryMovementRepository;
import com.tiendalaesquina.domain.repository.ProductRepository;
import com.tiendalaesquina.domain.repository.StockEntryItemRepository;
import com.tiendalaesquina.domain.repository.StockEntryRepository;
import com.tiendalaesquina.domain.repository.SupplierRepository;
import com.tiendalaesquina.domain.repository.UserAccountRepository;
import com.tiendalaesquina.exception.ApiException;
import com.tiendalaesquina.web.dto.CreateStockEntryItemRequest;
import com.tiendalaesquina.web.dto.CreateStockEntryRequest;
import com.tiendalaesquina.web.dto.StockEntryItemResponse;
import com.tiendalaesquina.web.dto.StockEntryResponse;
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

@Service
public class StockEntryService {

    private static final ZoneId GUATEMALA_ZONE = ZoneId.of("America/Guatemala");

    private final StockEntryRepository stockEntries;
    private final StockEntryItemRepository stockEntryItems;
    private final SupplierRepository suppliers;
    private final ProductRepository products;
    private final UserAccountRepository users;
    private final InventoryMovementRepository movements;
    private final AuditEventRepository auditEvents;

    public StockEntryService(
            StockEntryRepository stockEntries,
            StockEntryItemRepository stockEntryItems,
            SupplierRepository suppliers,
            ProductRepository products,
            UserAccountRepository users,
            InventoryMovementRepository movements,
            AuditEventRepository auditEvents
    ) {
        this.stockEntries = stockEntries;
        this.stockEntryItems = stockEntryItems;
        this.suppliers = suppliers;
        this.products = products;
        this.users = users;
        this.movements = movements;
        this.auditEvents = auditEvents;
    }

    @Transactional
    public StockEntryResponse create(CreateStockEntryRequest request, String actorEmail) {
        UserAccount actor = requireActor(actorEmail);
        Supplier supplier = requireActiveSupplier(request.supplierId());

        validateNoDuplicateProducts(request.items());

        Instant entryDate = request.entryDate()
                .atStartOfDay(GUATEMALA_ZONE)
                .toInstant();

        StockEntry entry = new StockEntry(
                supplier,
                entryDate,
                normalizeNullable(request.documentNumber()),
                normalizeNullable(request.notes()),
                actor
        );

        entry = stockEntries.save(entry);

        List<StockEntryItemResponse> itemResponses = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CreateStockEntryItemRequest itemRequest : request.items()) {
            Product product = requireActiveProductForUpdate(itemRequest.productId());

            validateQuantity(product, itemRequest.quantity());

            BigDecimal quantity = itemRequest.quantity()
                    .setScale(3, RoundingMode.UNNECESSARY);

            BigDecimal unitCost = itemRequest.unitCost()
                    .setScale(4, RoundingMode.UNNECESSARY);

            BigDecimal lineTotal = quantity
                    .multiply(unitCost)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal quantityBefore = product.getCurrentStock();

            BigDecimal quantityAfter = quantityBefore
                    .add(quantity)
                    .setScale(3, RoundingMode.UNNECESSARY);

            product.setCurrentStock(quantityAfter, actor);
            product.setPurchasePrice(unitCost, actor);
            products.save(product);

            StockEntryItem item = new StockEntryItem(
                    entry,
                    product,
                    quantity,
                    unitCost,
                    lineTotal
            );

            item = stockEntryItems.save(item);

            movements.save(new InventoryMovement(
                    product,
                    InventoryMovementType.STOCK_ENTRY,
                    quantity,
                    quantityBefore,
                    quantityAfter,
                    InventorySourceType.STOCK_ENTRY,
                    entry.getId(),
                    "Entrada de mercaderia"
                            + (entry.getDocumentNumber() == null
                            ? ""
                            : " - " + entry.getDocumentNumber()),
                    actor,
                    Instant.now()
            ));

            itemResponses.add(toItemResponse(item));
            totalAmount = totalAmount.add(lineTotal);
        }

        totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);

        auditEvents.save(new AuditEvent(
                actor,
                "STOCK_ENTRY_CREATED",
                "STOCK_ENTRY",
                String.valueOf(entry.getId()),
                "Entrada de mercaderia registrada: ID " + entry.getId(),
                null,
                Map.of(
                        "supplierId", supplier.getId(),
                        "supplierName", supplier.getName(),
                        "items", itemResponses.size(),
                        "totalAmount", totalAmount
                ),
                null,
                null,
                Instant.now()
        ));

        return toResponse(entry, itemResponses, totalAmount);
    }

    @Transactional(readOnly = true)
    public StockEntryResponse get(Long id) {
        StockEntry entry = requireStockEntry(id);

        List<StockEntryItem> items =
                stockEntryItems.findByStockEntryIdOrderByIdAsc(id);

        return toResponse(
                entry,
                items.stream().map(this::toItemResponse).toList(),
                calculateTotal(items)
        );
    }

    @Transactional(readOnly = true)
    public Page<StockEntryResponse> search(
            Long supplierId,
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

        Specification<StockEntry> specification =
                (root, query, cb) -> cb.conjunction();

        if (supplierId != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("supplier").get("id"), supplierId)
            );
        }

        if (fromDate != null) {
            Instant from = fromDate
                    .atStartOfDay(GUATEMALA_ZONE)
                    .toInstant();

            specification = specification.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("entryDate"), from)
            );
        }

        if (toDate != null) {
            Instant untilExclusive = toDate
                    .plusDays(1)
                    .atStartOfDay(GUATEMALA_ZONE)
                    .toInstant();

            specification = specification.and((root, query, cb) ->
                    cb.lessThan(root.get("entryDate"), untilExclusive)
            );
        }

        String normalizedReference = normalizeNullable(reference);

        if (normalizedReference != null) {
            String pattern =
                    "%" + normalizedReference.toLowerCase(Locale.ROOT) + "%";

            specification = specification.and((root, query, cb) ->
                    cb.like(
                            cb.lower(root.get("documentNumber")),
                            pattern
                    )
            );
        }

        return stockEntries.findAll(specification, pageable)
                .map(entry -> {
                    List<StockEntryItem> items =
                            stockEntryItems.findByStockEntryIdOrderByIdAsc(entry.getId());

                    return toResponse(
                            entry,
                            items.stream().map(this::toItemResponse).toList(),
                            calculateTotal(items)
                    );
                });
    }

    private Supplier requireActiveSupplier(Long id) {
        Supplier supplier = suppliers.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "supplier_not_found",
                        "Proveedor no encontrado",
                        "El proveedor seleccionado no existe"
                ));

        if (!supplier.isActive()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "supplier_inactive",
                    "Proveedor inactivo",
                    "No se puede registrar una entrada con un proveedor inactivo"
            );
        }

        return supplier;
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
                    "No se puede recibir mercaderia para un producto inactivo"
            );
        }

        return product;
    }

    private StockEntry requireStockEntry(Long id) {
        return stockEntries.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "stock_entry_not_found",
                        "Entrada no encontrada",
                        "La entrada de mercaderia solicitada no existe"
                ));
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

    private void validateNoDuplicateProducts(
            List<CreateStockEntryItemRequest> items
    ) {
        Set<Long> productIds = new HashSet<>();

        for (CreateStockEntryItemRequest item : items) {
            if (!productIds.add(item.productId())) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "duplicate_product_in_entry",
                        "Producto duplicado",
                        "Un producto no puede aparecer mas de una vez en la misma entrada"
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

    private BigDecimal calculateTotal(List<StockEntryItem> items) {
        return items.stream()
                .map(StockEntryItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private StockEntryItemResponse toItemResponse(StockEntryItem item) {
        Product product = item.getProduct();

        return new StockEntryItemResponse(
                item.getId(),
                product.getId(),
                product.getCode(),
                product.getName(),
                item.getQuantity(),
                item.getUnitCost(),
                item.getLineTotal()
        );
    }

    private StockEntryResponse toResponse(
            StockEntry entry,
            List<StockEntryItemResponse> items,
            BigDecimal totalAmount
    ) {
        Supplier supplier = entry.getSupplier();
        UserAccount actor = entry.getCreatedBy();

        return new StockEntryResponse(
                entry.getId(),
                supplier == null ? null : supplier.getId(),
                supplier == null ? null : supplier.getName(),
                entry.getEntryDate(),
                entry.getDocumentNumber(),
                entry.getNotes(),
                entry.getStatus().name(),
                actor.getId(),
                actor.getEmail(),
                entry.getConfirmedAt(),
                totalAmount,
                items,
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}