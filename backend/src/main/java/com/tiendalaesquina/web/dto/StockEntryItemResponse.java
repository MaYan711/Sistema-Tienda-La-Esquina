package com.tiendalaesquina.web.dto;

import java.math.BigDecimal;

public record StockEntryItemResponse(
        Long id,
        Long productId,
        String productCode,
        String productName,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal lineTotal
) {
}