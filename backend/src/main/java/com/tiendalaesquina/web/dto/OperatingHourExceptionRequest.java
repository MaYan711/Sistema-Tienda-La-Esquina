package com.tiendalaesquina.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record OperatingHourExceptionRequest(
        @NotNull LocalDate date,
        boolean closed,
        LocalTime openingTime,
        LocalTime closingTime,
        @Size(max = 160) String reason
) {
}
