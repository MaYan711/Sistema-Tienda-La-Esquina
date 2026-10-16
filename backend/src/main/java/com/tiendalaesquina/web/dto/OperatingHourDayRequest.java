package com.tiendalaesquina.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record OperatingHourDayRequest(
        @NotBlank String dayOfWeek,
        boolean enabled,
        @NotNull LocalTime openingTime,
        @NotNull LocalTime closingTime
) {
}
