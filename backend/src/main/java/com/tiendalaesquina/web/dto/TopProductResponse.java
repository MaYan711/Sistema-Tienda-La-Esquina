package com.tiendalaesquina.web.dto;

import java.math.BigDecimal;

public record TopProductResponse(
        Long productId,
        String productCode,
        String productName,
        BigDecimal quantitySold,
        BigDecimal totalSales
) {
}