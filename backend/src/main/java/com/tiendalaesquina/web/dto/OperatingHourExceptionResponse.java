package com.tiendalaesquina.web.dto;

import java.time.Instant;
import java.time.LocalDate;

public record OperatingHourExceptionResponse(
        Long id,
        LocalDate date,
        boolean closed,
        String openingTime,
        String closingTime,
        String reason,
        String updatedByEmail,
        Instant updatedAt
) {
}
