package com.tiendalaesquina.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Petición para realizar un ajuste de existencias de un producto")
public record InventoryAdjustmentRequest(
        @NotNull(message = "El id del producto es obligatorio")
        @Schema(description = "Identificador único del producto a ajustar", example = "1")
        Long productId,

        @NotNull(message = "El nuevo stock es obligatorio")
        @DecimalMin(value = "0.0", message = "El stock no puede ser negativo")
        @Digits(integer = 11, fraction = 3, message = "El stock debe tener como máximo 11 dígitos enteros y 3 decimales")
        @Schema(description = "Nueva existencia física del producto (no negativa, máximo 3 decimales)", example = "25.000")
        BigDecimal newStock,

        @NotBlank(message = "El motivo del ajuste es obligatorio")
        @Size(max = 500, message = "El motivo no puede exceder 500 caracteres")
        @Schema(description = "Motivo detallado por el que se realiza el ajuste de inventario", example = "Conteo físico mensual de bodega")
        String reason
) {
}
