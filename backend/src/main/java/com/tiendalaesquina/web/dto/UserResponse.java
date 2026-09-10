package com.tiendalaesquina.web.dto;

public record UserResponse(
    Long id,
    String email,
    String role,
    boolean verified,
    boolean twoFactorEnabled
) {
}
