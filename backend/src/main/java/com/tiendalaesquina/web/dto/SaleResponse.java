package com.tiendalaesquina.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SaleResponse(
        Long id,
        String saleNumber,
        Long soldById,
        String soldByEmail,
        Instant saleDate,
        BigDecimal subtotalAmount,
        BigDecimal totalAmount,
        BigDecimal cashReceived,
        BigDecimal changeAmount,
        String status,
        String notes,
        List<SaleItemResponse> items,
        Instant createdAt
) {
}