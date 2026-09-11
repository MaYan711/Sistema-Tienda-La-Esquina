package com.tiendalaesquina.web.dto;

import java.time.Instant;

public record MeasurementUnitResponse(
    Long id,
    String code,
    String name,
    boolean allowsDecimal,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
