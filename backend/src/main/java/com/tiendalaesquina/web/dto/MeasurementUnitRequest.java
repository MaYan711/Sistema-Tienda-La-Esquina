package com.tiendalaesquina.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MeasurementUnitRequest(
    @NotBlank
    @Size(max = 16)
    String code,
    @NotBlank
    @Size(max = 64)
    String name,
    boolean allowsDecimal
) {
}
