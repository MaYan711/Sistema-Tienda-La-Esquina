package com.tiendalaesquina.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductsSoldReportResponse(
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal productsSold,
        BigDecimal previousPeriodProductsSold,
        BigDecimal changePercentage
) {
}