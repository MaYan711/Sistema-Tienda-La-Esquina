package com.tiendalaesquina.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateStockEntryItemRequest(

        @NotNull
        @Positive
        Long productId,

        @NotNull
        @DecimalMin(value = "0.001")
        @Digits(integer = 11, fraction = 3)
        BigDecimal quantity,

        @NotNull
        @DecimalMin(value = "0.0001")
        @Digits(integer = 10, fraction = 4)
        BigDecimal unitCost
) {
}