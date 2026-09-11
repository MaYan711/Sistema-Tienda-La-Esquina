package com.tiendalaesquina.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record InventoryMovementResponse(
        Long id,
        Long productId,
        String productCode,
        String productName,
        String movementType,
        BigDecimal quantityDelta,
        BigDecimal quantityBefore,
        BigDecimal quantityAfter,
        String sourceType,
        Long sourceId,
        String reason,
        String createdByEmail,
        Instant createdAt
) {
}
