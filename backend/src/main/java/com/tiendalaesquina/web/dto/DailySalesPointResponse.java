package com.tiendalaesquina.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailySalesPointResponse(
        LocalDate date,
        long salesCount,
        BigDecimal totalSales
) {
}