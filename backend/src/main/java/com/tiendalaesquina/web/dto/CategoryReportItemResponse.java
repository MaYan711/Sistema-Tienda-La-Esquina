package com.tiendalaesquina.web.dto;

import java.math.BigDecimal;

public record CategoryReportItemResponse(
        Long categoryId,
        String categoryName,
        BigDecimal productsSold,
        BigDecimal totalSales,
        BigDecimal estimatedMargin,
        BigDecimal percentageOfSales
) {
}