package com.tiendalaesquina.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record SupplierPriceComparisonResponse(
        Long productId,
        String productCode,
        String productName,
        BigDecimal currentPurchasePrice,
        int suppliersCompared,
        Long recommendedSupplierId,
        String recommendedSupplierName,
        BigDecimal recommendedUnitCost,
        BigDecimal highestUnitCost,
        BigDecimal savingsPerUnit,
        BigDecimal savingsPercentage,
        String recommendation,
        List<SupplierPriceComparisonItemResponse> suppliers
) {
}
