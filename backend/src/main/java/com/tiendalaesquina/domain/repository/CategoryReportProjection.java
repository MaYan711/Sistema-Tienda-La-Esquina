package com.tiendalaesquina.domain.repository;

import java.math.BigDecimal;

public interface CategoryReportProjection {

    Long getCategoryId();

    String getCategoryName();

    BigDecimal getProductsSold();

    BigDecimal getTotalSales();

    BigDecimal getEstimatedMargin();
}