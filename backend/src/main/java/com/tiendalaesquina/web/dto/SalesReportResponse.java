package com.tiendalaesquina.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesReportResponse(
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal totalSales,
        long salesCount,
        BigDecimal previousPeriodTotal,
        BigDecimal changePercentage
) {
}