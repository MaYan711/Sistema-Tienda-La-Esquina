package com.tiendalaesquina.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateSaleRequest(
        @NotNull(message = "El efectivo recibido es obligatorio")
        @DecimalMin(value = "0.00", message = "El efectivo recibido no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "El efectivo recibido no tiene un formato valido")
        BigDecimal cashReceived,

        @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
        String notes,

        @NotEmpty(message = "Debe agregar al menos un producto")
        List<@Valid CreateSaleItemRequest> items
) {
}