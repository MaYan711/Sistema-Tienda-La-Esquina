package com.tiendalaesquina.application.inventory;

import com.tiendalaesquina.domain.model.StockNotificationType;

import java.math.BigDecimal;

public record StockAlertEmailEvent(
        StockNotificationType notificationType,
        Long productId,
        String productCode,
        String productName,
        BigDecimal currentStock,
        BigDecimal minimumStock
) {
}
