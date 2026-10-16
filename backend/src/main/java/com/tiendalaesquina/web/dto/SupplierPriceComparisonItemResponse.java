package com.tiendalaesquina.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SupplierPriceComparisonItemResponse(
        Long supplierId,
        String supplierName,
        String supplierNit,
        BigDecimal latestUnitCost,
        BigDecimal averageUnitCost,
        BigDecimal minimumUnitCost,
        BigDecimal maximumUnitCost,
        long purchaseCount,
        Instant lastPurchaseDate,
        BigDecimal differenceFromBest,
        BigDecimal percentageAboveBest,
        boolean recommended
) {
}
