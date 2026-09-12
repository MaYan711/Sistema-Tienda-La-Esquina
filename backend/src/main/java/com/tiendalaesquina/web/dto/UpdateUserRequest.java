package com.tiendalaesquina.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico no es válido")
    @Schema(example = "empleado@tiendalaesquina.com")
    String email,

    @NotBlank(message = "El rol es obligatorio")
    @Schema(example = "EMPLOYEE", allowableValues = {"ADMIN", "EMPLOYEE"})
    String role
) {
}
