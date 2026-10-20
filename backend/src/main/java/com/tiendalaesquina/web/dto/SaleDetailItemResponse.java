package com.tiendalaesquina.web.dto;

import java.math.BigDecimal;

public record SaleDetailItemResponse(
        Long id,
        Long productId,
        String productCode,
        String productName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
