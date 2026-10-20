package com.tiendalaesquina.web.dto;

import java.math.BigDecimal;

public record SaleReceiptItemResponse(
        Long productId,
        String productCode,
        String productName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
