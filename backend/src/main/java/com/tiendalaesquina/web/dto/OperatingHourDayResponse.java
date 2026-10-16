package com.tiendalaesquina.web.dto;

public record OperatingHourDayResponse(
        String dayOfWeek,
        String label,
        boolean enabled,
        String openingTime,
        String closingTime,
        boolean configured
) {
}
