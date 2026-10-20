package com.tiendalaesquina.web.dto;

import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;

public record UserResponse(
    @Schema(example = "1")
    Long id,

    @Schema(example = "empleado@tiendalaesquina.com")
    String email,

    @Schema(example = "EMPLOYEE")
    String role,

    @Schema(example = "true")
    boolean verified,

    @Schema(example = "true")
    boolean enabled,

    @Schema(example = "false")
    boolean twoFactorEnabled,

    @Schema(example = "2026-09-11T12:00:00Z")
    Instant createdAt,

    @Schema(example = "2026-09-11T12:00:00Z")
    Instant updatedAt
) {
}
