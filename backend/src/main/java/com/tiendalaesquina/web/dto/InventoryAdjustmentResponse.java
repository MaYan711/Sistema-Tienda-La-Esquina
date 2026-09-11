package com.tiendalaesquina.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Respuesta tras procesar un ajuste de inventario")
public record InventoryAdjustmentResponse(
        @Schema(description = "Identificador único del registro de ajuste", example = "10")
        Long id,

        @Schema(description = "Identificador del producto ajustado", example = "1")
        Long productId,

        @Schema(description = "Código del producto", example = "BEB001")
        String productCode,

        @Schema(description = "Nombre del producto", example = "Coca-Cola 600 ml")
        String productName,

        @Schema(description = "Existencia antes del ajuste", example = "24.000")
        BigDecimal quantityBefore,

        @Schema(description = "Nueva existencia tras el ajuste", example = "25.000")
        BigDecimal quantityAfter,

        @Schema(description = "Diferencia aplicada al inventario", example = "1.000")
        BigDecimal quantityDelta,

        @Schema(description = "Motivo del ajuste", example = "Conteo físico mensual de bodega")
        String reason,

        @Schema(description = "Correo del usuario administrador responsable", example = "admin@tiendalaesquina.local")
        String adjustedByEmail,

        @Schema(description = "Fecha y hora del ajuste", example = "2026-09-11T08:00:00Z")
        Instant adjustmentDate,

        @Schema(description = "Fecha y hora de creación del registro", example = "2026-09-11T08:00:00Z")
        Instant createdAt,

        @Schema(description = "Estado resultante de inventario (AVAILABLE, LOW_STOCK, OUT_OF_STOCK)", example = "AVAILABLE")
        String stockStatus
) {
}
