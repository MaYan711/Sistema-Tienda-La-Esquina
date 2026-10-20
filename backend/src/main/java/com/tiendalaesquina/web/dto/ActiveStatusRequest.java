package com.tiendalaesquina.web.dto;

import jakarta.validation.constraints.NotNull;

public record ActiveStatusRequest(
    @NotNull Boolean active
) {
}
