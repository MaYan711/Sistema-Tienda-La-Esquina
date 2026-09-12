package com.tiendalaesquina.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(
    @NotNull(message = "El campo 'enabled' es obligatorio")
    @Schema(example = "true", description = "Estado habilitado del usuario")
    Boolean enabled
) {
}
