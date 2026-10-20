package com.tiendalaesquina.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateSupplierRequest(

        @NotBlank
        @Size(max = 160)
        String name,

        @Pattern(regexp = "^\\d{9}$", message = "El NIT debe contener exactamente 9 digitos")
        String nit,

        @Pattern(regexp = "^\\d{8}$", message = "El telefono debe contener exactamente 8 digitos")
        String phone,

        @Email
        @Size(max = 160)
        String email,

        @Size(max = 255)
        String address
) {
}