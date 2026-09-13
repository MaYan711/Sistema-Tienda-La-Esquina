package com.tiendalaesquina.web.dto;

import java.math.BigDecimal;

public record SaleItemResponse(
        Long id,
        Long productId,
        String productCode,
        String productName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal unitCostSnapshot,
        BigDecimal lineTotal
) {
}