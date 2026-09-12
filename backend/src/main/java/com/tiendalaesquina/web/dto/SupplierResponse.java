package com.tiendalaesquina.web.dto;

import java.time.Instant;

public record SupplierResponse(
        Long id,
        String name,
        String nit,
        String phone,
        String email,
        String address,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}