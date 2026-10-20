package com.tiendalaesquina.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateSaleItemRequest(
        @NotNull(message = "El id del producto es obligatorio")
        Long productId,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor que cero")
        @Digits(integer = 11, fraction = 3, message = "La cantidad no tiene un formato valido")
        BigDecimal quantity
) {
}