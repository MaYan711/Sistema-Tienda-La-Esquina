package com.tiendalaesquina.domain.repository;

import java.math.BigDecimal;

public interface TopProductProjection {

    Long getProductId();

    String getProductCode();

    String getProductName();

    BigDecimal getQuantitySold();

    BigDecimal getTotalSales();
}