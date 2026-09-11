package com.tiendalaesquina.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(
    @NotBlank
    @Size(max = 64)
    @Schema(example = "BEB065")
    String code,
    @NotBlank
    @Size(max = 160)
    @Schema(example = "Tiky Naranja 600 ml")
    String name,
    @Size(max = 500)
    String description,
    @Size(max = 500)
    String imageUrl,
    @NotNull
    Long categoryId,
    @NotNull
    Long unitId,
    @NotNull
    @DecimalMin("0.00")
    BigDecimal purchasePrice,
    @NotNull
    @DecimalMin("0.00")
    BigDecimal salePrice,
    @NotNull
    @DecimalMin("0.000")
    BigDecimal initialStock,
    @NotNull
    @DecimalMin("0.000")
    BigDecimal minimumStock
) {
}
