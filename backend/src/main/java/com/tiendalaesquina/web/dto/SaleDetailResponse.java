package com.tiendalaesquina.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleDetailResponse(
        Long id,
        String saleNumber,
        Instant saleDate,
        Long soldById,
        String soldByEmail,
        String paymentMethod,
        String status,
        int differentProducts,
        BigDecimal totalUnits,
        BigDecimal subtotalAmount,
        BigDecimal totalAmount,
        BigDecimal cashReceived,
        BigDecimal changeAmount,
        String notes,
        List<SaleDetailItemResponse> items,
        Instant createdAt
) {
}
