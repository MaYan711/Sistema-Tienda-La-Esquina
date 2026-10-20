package com.tiendalaesquina.web.dto;

import java.time.LocalDate;
import java.util.List;

public record DailySalesReportResponse(
        LocalDate fromDate,
        LocalDate toDate,
        List<DailySalesPointResponse> dailySales
) {
}