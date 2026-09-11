package com.tiendalaesquina.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VerifyChallengeRequest(
    @NotNull(message = "El identificador del desafío es obligatorio")
    UUID challengeId,
    @NotBlank(message = "El código OTP es obligatorio")
    @Schema(example = "123456")
    String otp
) {
}
