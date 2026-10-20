package com.tiendalaesquina.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record StockNotificationResponse(
        Long id,
        Long productId,
        String productCode,
        String productName,
        String type,
        String title,
        String message,
        BigDecimal currentStock,
        BigDecimal minimumStock,
        boolean read,
        String readByEmail,
        Instant readAt,
        Instant createdAt
) {
}
