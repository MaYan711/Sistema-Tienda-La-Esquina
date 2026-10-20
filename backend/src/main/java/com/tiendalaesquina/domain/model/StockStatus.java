package com.tiendalaesquina.domain.model;

import java.math.BigDecimal;

public enum StockStatus {
    AVAILABLE,
    LOW_STOCK,
    OUT_OF_STOCK;

    public static StockStatus from(BigDecimal currentStock, BigDecimal minimumStock) {
        if (currentStock == null || currentStock.compareTo(BigDecimal.ZERO) <= 0) {
            return OUT_OF_STOCK;
        }
        if (minimumStock != null && currentStock.compareTo(minimumStock) <= 0) {
            return LOW_STOCK;
        }
        return AVAILABLE;
    }
}
