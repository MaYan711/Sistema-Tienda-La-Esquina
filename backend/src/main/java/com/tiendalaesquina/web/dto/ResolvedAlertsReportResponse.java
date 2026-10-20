package com.tiendalaesquina.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResolvedAlertsReportResponse(
        LocalDate fromDate,
        LocalDate toDate,
        long resolvedAlerts,
        long previousPeriodResolvedAlerts,
        BigDecimal changePercentage
) {
}