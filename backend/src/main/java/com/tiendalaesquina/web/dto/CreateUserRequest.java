package com.tiendalaesquina.web.dto;

import com.tiendalaesquina.web.validation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico no es válido")
    @Schema(example = "empleado@tiendalaesquina.com")
    String email,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    @ValidPassword
    @Schema(example = "Password123", minLength = 8, maxLength = 72)
    String password,

    @NotBlank(message = "El rol es obligatorio")
    @Schema(example = "EMPLOYEE", allowableValues = {"ADMIN", "EMPLOYEE"})
    String role
) {
}
