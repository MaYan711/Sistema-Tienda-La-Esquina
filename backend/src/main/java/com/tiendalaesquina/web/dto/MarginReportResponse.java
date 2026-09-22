package com.tiendalaesquina.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MarginReportResponse(
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal estimatedMargin,
        BigDecimal previousPeriodMargin,
        BigDecimal changePercentage
) {
}