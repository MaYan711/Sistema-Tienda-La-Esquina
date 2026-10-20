package com.tiendalaesquina.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleReceiptResponse(
        String storeName,
        String documentTitle,
        Long saleId,
        String saleNumber,
        Instant saleDate,
        String soldByEmail,
        String paymentMethod,
        BigDecimal subtotalAmount,
        BigDecimal totalAmount,
        BigDecimal cashReceived,
        BigDecimal changeAmount,
        String notes,
        List<SaleReceiptItemResponse> items
) {
}
