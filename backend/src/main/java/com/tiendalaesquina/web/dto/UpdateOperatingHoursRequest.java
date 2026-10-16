package com.tiendalaesquina.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateOperatingHoursRequest(
        @NotEmpty List<@Valid OperatingHourDayRequest> days
) {
}
