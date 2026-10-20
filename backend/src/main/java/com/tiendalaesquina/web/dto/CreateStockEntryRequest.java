package com.tiendalaesquina.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateStockEntryRequest(

        @NotNull
        @Positive
        Long supplierId,

        @NotNull
        LocalDate entryDate,

        @Size(max = 80)
        String documentNumber,

        @Size(max = 500)
        String notes,

        @NotEmpty
        List<@Valid CreateStockEntryItemRequest> items
) {
}