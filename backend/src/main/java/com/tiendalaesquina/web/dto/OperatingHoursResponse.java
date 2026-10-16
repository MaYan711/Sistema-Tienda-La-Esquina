package com.tiendalaesquina.web.dto;

public record OperatingHoursResponse(
        boolean enabled,
        String timeZone,
        String openingTime,
        String closingTime,
        String currentDate,
        String currentTime,
        String role,
        boolean employeeRestricted,
        boolean operationAllowed,
        boolean closedToday,
        String scheduleSource,
        String reason,
        String message
) {
}
