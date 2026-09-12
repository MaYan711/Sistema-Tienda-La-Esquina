package com.tiendalaesquina.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSupplierRequest(

        @NotBlank
        @Size(max = 160)
        String name,

        @Size(max = 32)
        String nit,

        @Size(max = 32)
        String phone,

        @Email
        @Size(max = 160)
        String email,

        @Size(max = 255)
        String address
) {
}