package com.tiendalaesquina.web.dto;

import java.time.LocalDate;
import java.util.List;

public record CategoryReportResponse(
        LocalDate fromDate,
        LocalDate toDate,
        List<CategoryReportItemResponse> categories
) {
}