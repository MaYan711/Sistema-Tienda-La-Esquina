package com.tiendalaesquina.web.dto;

import java.time.LocalDate;
import java.util.List;

public record TopProductsReportResponse(
        LocalDate fromDate,
        LocalDate toDate,
        List<TopProductResponse> products
) {
}