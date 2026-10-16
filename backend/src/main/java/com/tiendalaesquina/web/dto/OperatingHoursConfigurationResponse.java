package com.tiendalaesquina.web.dto;

import java.util.List;

public record OperatingHoursConfigurationResponse(
        boolean enabled,
        String timeZone,
        String defaultOpeningTime,
        String defaultClosingTime,
        List<OperatingHourDayResponse> weekly,
        List<OperatingHourExceptionResponse> exceptions
) {
}
