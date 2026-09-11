package com.tiendalaesquina.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
    Long id,
    String code,
    String name,
    String description,
    String imageUrl,
    CategoryResponse category,
    MeasurementUnitResponse unit,
    BigDecimal purchasePrice,
    BigDecimal salePrice,
    BigDecimal currentStock,
    BigDecimal minimumStock,
    String stockStatus,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
