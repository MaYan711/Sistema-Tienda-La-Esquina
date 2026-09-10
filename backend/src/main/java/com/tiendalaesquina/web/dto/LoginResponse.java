package com.tiendalaesquina.web.dto;

import java.util.UUID;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    boolean requiresTwoFactor,
    UUID challengeId,
    String message
) {

    public static LoginResponse token(String accessToken, long expiresIn) {
        return new LoginResponse(accessToken, "Bearer", expiresIn, false, null,
            "Autenticacion exitosa");
    }

    public static LoginResponse challenge(UUID challengeId) {
        return new LoginResponse(null, null, 0, true, challengeId,
            "Se requiere el código OTP de inicio de sesión");
    }
}
