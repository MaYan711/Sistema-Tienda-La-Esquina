package com.tiendalaesquina.application.stockentry;

import com.tiendalaesquina.domain.model.Product;
import com.tiendalaesquina.domain.model.StockEntryItem;
import com.tiendalaesquina.domain.model.StockEntryStatus;
import com.tiendalaesquina.domain.model.Supplier;
import com.tiendalaesquina.domain.repository.ProductRepository;
import com.tiendalaesquina.domain.repository.StockEntryItemRepository;
import com.tiendalaesquina.exception.ApiException;
import com.tiendalaesquina.web.dto.SupplierPriceComparisonItemResponse;
import com.tiendalaesquina.web.dto.SupplierPriceComparisonResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SupplierPriceComparisonService {

    private final ProductRepository products;
    private final StockEntryItemRepository stockEntryItems;

    public SupplierPriceComparisonService(
            ProductRepository products,
            StockEntryItemRepository stockEntryItems
    ) {
        this.products = products;
        this.stockEntryItems = stockEntryItems;
    }

    @Transactional(readOnly = true)
    public SupplierPriceComparisonResponse compare(Long productId) {
        Product product = products.findById(productId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "product_not_found",
                        "Producto no encontrado",
                        "El producto seleccionado no existe"
                ));

        List<StockEntryItem> history = stockEntryItems.findPriceHistoryForProduct(
                productId,
                StockEntryStatus.CONFIRMED
        );

        if (history.isEmpty()) {
            return emptyResponse(product);
        }

        Map<Long, SupplierAccumulator> bySupplier = new LinkedHashMap<>();

        for (StockEntryItem item : history) {
            Supplier supplier = item.getStockEntry().getSupplier();

            if (supplier == null) {
                continue;
            }

            bySupplier.computeIfAbsent(
                    supplier.getId(),
                    ignored -> new SupplierAccumulator(
                            supplier,
                            item.getUnitCost(),
                            item.getStockEntry().getEntryDate()
                    )
            ).add(item.getUnitCost());
        }

        if (bySupplier.isEmpty()) {
            return emptyResponse(product);
        }

        List<SupplierAccumulator> accumulators = new ArrayList<>(bySupplier.values());
        accumulators.sort(
                Comparator.comparing(SupplierAccumulator::latestUnitCost)
                        .thenComparing(accumulator -> accumulator.supplier().getName(), String.CASE_INSENSITIVE_ORDER)
        );

        SupplierAccumulator best = accumulators.get(0);
        BigDecimal bestCost = money4(best.latestUnitCost());
        BigDecimal highestCost = money4(
                accumulators.stream()
                        .map(SupplierAccumulator::latestUnitCost)
                        .max(BigDecimal::compareTo)
                        .orElse(bestCost)
        );

        BigDecimal savingsPerUnit = money4(highestCost.subtract(bestCost));
        BigDecimal savingsPercentage = percentage(savingsPerUnit, highestCost);

        List<SupplierPriceComparisonItemResponse> comparisons = accumulators.stream()
                .map(accumulator -> {
                    BigDecimal latestCost = money4(accumulator.latestUnitCost());
                    BigDecimal difference = money4(latestCost.subtract(bestCost));
                    BigDecimal percentageAboveBest = percentage(difference, bestCost);

                    return new SupplierPriceComparisonItemResponse(
                            accumulator.supplier().getId(),
                            accumulator.supplier().getName(),
                            accumulator.supplier().getNit(),
                            latestCost,
                            money4(accumulator.average()),
                            money4(accumulator.minimumUnitCost()),
                            money4(accumulator.maximumUnitCost()),
                            accumulator.purchaseCount(),
                            accumulator.lastPurchaseDate(),
                            difference,
                            percentageAboveBest,
                            accumulator.supplier().getId().equals(best.supplier().getId())
                    );
                })
                .toList();

        String recommendation;

        if (comparisons.size() == 1) {
            recommendation = "Solo existe historial de compra con "
                    + best.supplier().getName()
                    + ". Registra el mismo producto con otro proveedor para obtener una comparativa.";
        } else if (savingsPerUnit.compareTo(BigDecimal.ZERO) == 0) {
            recommendation = "Los proveedores comparados tienen el mismo costo unitario mas reciente. "
                    + "Puedes decidir considerando disponibilidad, entrega y condiciones comerciales.";
        } else {
            recommendation = "Se recomienda comprar "
                    + product.getName()
                    + " a "
                    + best.supplier().getName()
                    + " porque registra el menor costo unitario reciente (Q "
                    + bestCost.setScale(2, RoundingMode.HALF_UP)
                    + "). El ahorro puede ser de Q "
                    + savingsPerUnit.setScale(2, RoundingMode.HALF_UP)
                    + " por unidad frente al proveedor mas caro.";
        }

        return new SupplierPriceComparisonResponse(
                product.getId(),
                product.getCode(),
                product.getName(),
                money4(product.getPurchasePrice()),
                comparisons.size(),
                best.supplier().getId(),
                best.supplier().getName(),
                bestCost,
                highestCost,
                savingsPerUnit,
                savingsPercentage,
                recommendation,
                comparisons
        );
    }

    private SupplierPriceComparisonResponse emptyResponse(Product product) {
        return new SupplierPriceComparisonResponse(
                product.getId(),
                product.getCode(),
                product.getName(),
                money4(product.getPurchasePrice()),
                0,
                null,
                null,
                null,
                null,
                BigDecimal.ZERO.setScale(4),
                BigDecimal.ZERO.setScale(2),
                "Aun no existen entradas confirmadas para este producto. Registra compras para poder comparar proveedores.",
                List.of()
        );
    }

    private BigDecimal money4(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(4)
                : value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(BigDecimal difference, BigDecimal base) {
        if (base == null || base.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2);
        }

        return difference
                .multiply(BigDecimal.valueOf(100))
                .divide(base, 2, RoundingMode.HALF_UP);
    }

    private static final class SupplierAccumulator {
        private final Supplier supplier;
        private final BigDecimal latestUnitCost;
        private final Instant lastPurchaseDate;
        private BigDecimal total = BigDecimal.ZERO;
        private BigDecimal minimumUnitCost;
        private BigDecimal maximumUnitCost;
        private long purchaseCount;

        private SupplierAccumulator(
                Supplier supplier,
                BigDecimal latestUnitCost,
                Instant lastPurchaseDate
        ) {
            this.supplier = supplier;
            this.latestUnitCost = latestUnitCost;
            this.lastPurchaseDate = lastPurchaseDate;
        }

        private void add(BigDecimal unitCost) {
            total = total.add(unitCost);
            purchaseCount++;

            if (minimumUnitCost == null || unitCost.compareTo(minimumUnitCost) < 0) {
                minimumUnitCost = unitCost;
            }

            if (maximumUnitCost == null || unitCost.compareTo(maximumUnitCost) > 0) {
                maximumUnitCost = unitCost;
            }
        }

        private BigDecimal average() {
            return purchaseCount == 0
                    ? BigDecimal.ZERO
                    : total.divide(BigDecimal.valueOf(purchaseCount), 4, RoundingMode.HALF_UP);
        }

        private Supplier supplier() {
            return supplier;
        }

        private BigDecimal latestUnitCost() {
            return latestUnitCost;
        }

        private Instant lastPurchaseDate() {
            return lastPurchaseDate;
        }

        private BigDecimal minimumUnitCost() {
            return minimumUnitCost;
        }

        private BigDecimal maximumUnitCost() {
            return maximumUnitCost;
        }

        private long purchaseCount() {
            return purchaseCount;
        }
    }
}
