package com.tiendalaesquina.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record StockEntryResponse(
        Long id,

        Long supplierId,
        String supplierName,

        Instant entryDate,

        String documentNumber,
        String notes,

        String status,

        Long createdById,
        String createdByEmail,

        Instant confirmedAt,

        BigDecimal totalAmount,

        List<StockEntryItemResponse> items,

        Instant createdAt,
        Instant updatedAt
) {
}