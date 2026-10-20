package com.tiendalaesquina.application.report;

import com.tiendalaesquina.domain.model.SaleStatus;
import com.tiendalaesquina.domain.repository.SaleRepository;
import com.tiendalaesquina.web.dto.SalesReportResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tiendalaesquina.domain.repository.SaleItemRepository;
import com.tiendalaesquina.web.dto.MarginReportResponse;
import com.tiendalaesquina.web.dto.ProductsSoldReportResponse;
import com.tiendalaesquina.domain.repository.StockNotificationRepository;
import com.tiendalaesquina.web.dto.ResolvedAlertsReportResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import com.tiendalaesquina.domain.repository.TopProductProjection;
import com.tiendalaesquina.web.dto.TopProductResponse;
import com.tiendalaesquina.web.dto.TopProductsReportResponse;
import org.springframework.data.domain.PageRequest;

import com.tiendalaesquina.domain.model.Sale;
import com.tiendalaesquina.web.dto.DailySalesPointResponse;
import com.tiendalaesquina.web.dto.DailySalesReportResponse;

import com.tiendalaesquina.domain.repository.CategoryReportProjection;
import com.tiendalaesquina.web.dto.CategoryReportItemResponse;
import com.tiendalaesquina.web.dto.CategoryReportResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private static final ZoneId GUATEMALA_ZONE =
            ZoneId.of("America/Guatemala");

    private final SaleRepository sales;
    private final SaleItemRepository saleItems;
    private final StockNotificationRepository notifications;

    public ReportService(
            SaleRepository sales,
            SaleItemRepository saleItems,
            StockNotificationRepository notifications
    ) {
        this.sales = sales;
        this.saleItems = saleItems;
        this.notifications = notifications;
    }

    @Transactional(readOnly = true)
    public SalesReportResponse getSalesReport(
            LocalDate fromDate,
            LocalDate toDate
    ) {
        LocalDate resolvedFrom = fromDate;
        LocalDate resolvedTo = toDate;

        LocalDate today = LocalDate.now(GUATEMALA_ZONE);

        if (resolvedFrom == null) {
            resolvedFrom = today.withDayOfMonth(1);
        }

        if (resolvedTo == null) {
            resolvedTo = today;
        }

        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la fecha final."
            );
        }

        Instant from = resolvedFrom
                .atStartOfDay(GUATEMALA_ZONE)
                .toInstant();

        Instant toExclusive = resolvedTo
                .plusDays(1)
                .atStartOfDay(GUATEMALA_ZONE)
                .toInstant();

        BigDecimal totalSales = sales.sumTotalSales(
                SaleStatus.COMPLETED,
                from,
                toExclusive
        );

        long salesCount = sales.countSales(
                SaleStatus.COMPLETED,
                from,
                toExclusive
        );

        long numberOfDays =
                ChronoUnit.DAYS.between(resolvedFrom, resolvedTo) + 1;

        LocalDate previousTo =
                resolvedFrom.minusDays(1);

        LocalDate previousFrom =
                previousTo.minusDays(numberOfDays - 1);

        Instant previousFromInstant = previousFrom
                .atStartOfDay(GUATEMALA_ZONE)
                .toInstant();

        Instant previousToExclusive = resolvedFrom
                .atStartOfDay(GUATEMALA_ZONE)
                .toInstant();

        BigDecimal previousPeriodTotal = sales.sumTotalSales(
                SaleStatus.COMPLETED,
                previousFromInstant,
                previousToExclusive
        );

        BigDecimal changePercentage = calculatePercentageChange(
                totalSales,
                previousPeriodTotal
        );

        return new SalesReportResponse(
                resolvedFrom,
                resolvedTo,
                totalSales.setScale(2, RoundingMode.HALF_UP),
                salesCount,
                previousPeriodTotal.setScale(
                        2,
                        RoundingMode.HALF_UP
                ),
                changePercentage
        );


    }

    @Transactional(readOnly = true)
    public MarginReportResponse getMarginReport(
            LocalDate fromDate,
            LocalDate toDate
    ) {
        LocalDate resolvedFrom = fromDate;
        LocalDate resolvedTo = toDate;

        LocalDate today = LocalDate.now(GUATEMALA_ZONE);

        if (resolvedFrom == null) {
            resolvedFrom = today.withDayOfMonth(1);
        }

        if (resolvedTo == null) {
            resolvedTo = today;
        }

        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la fecha final."
            );
        }

        Instant from = resolvedFrom
                .atStartOfDay(GUATEMALA_ZONE)
                .toInstant();

        Instant toExclusive = resolvedTo
                .plusDays(1)
                .atStartOfDay(GUATEMALA_ZONE)
                .toInstant();

        BigDecimal estimatedMargin =
                saleItems.sumEstimatedMargin(
                        SaleStatus.COMPLETED,
                        from,
                        toExclusive
                );

        long numberOfDays =
                ChronoUnit.DAYS.between(
                        resolvedFrom,
                        resolvedTo
                ) + 1;

        LocalDate previousTo =
                resolvedFrom.minusDays(1);

        LocalDate previousFrom =
                previousTo.minusDays(numberOfDays - 1);

        Instant previousFromInstant =
                previousFrom
                        .atStartOfDay(GUATEMALA_ZONE)
                        .toInstant();

        Instant previousToExclusive =
                resolvedFrom
                        .atStartOfDay(GUATEMALA_ZONE)
                        .toInstant();

        BigDecimal previousPeriodMargin =
                saleItems.sumEstimatedMargin(
                        SaleStatus.COMPLETED,
                        previousFromInstant,
                        previousToExclusive
                );

        BigDecimal changePercentage =
                calculatePercentageChange(
                        estimatedMargin,
                        previousPeriodMargin
                );

        return new MarginReportResponse(
                resolvedFrom,
                resolvedTo,
                estimatedMargin.setScale(
                        2,
                        RoundingMode.HALF_UP
                ),
                previousPeriodMargin.setScale(
                        2,
                        RoundingMode.HALF_UP
                ),
                changePercentage
        );
    }

    @Transactional(readOnly = true)
    public ProductsSoldReportResponse getProductsSoldReport(
            LocalDate fromDate,
            LocalDate toDate
    ) {
        LocalDate resolvedFrom = fromDate;
        LocalDate resolvedTo = toDate;

        LocalDate today = LocalDate.now(GUATEMALA_ZONE);

        if (resolvedFrom == null) {
            resolvedFrom = today.withDayOfMonth(1);
        }

        if (resolvedTo == null) {
            resolvedTo = today;
        }

        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la fecha final."
            );
        }

        Instant from = resolvedFrom
                .atStartOfDay(GUATEMALA_ZONE)
                .toInstant();

        Instant toExclusive = resolvedTo
                .plusDays(1)
                .atStartOfDay(GUATEMALA_ZONE)
                .toInstant();

        BigDecimal productsSold =
                saleItems.sumProductsSold(
                        SaleStatus.COMPLETED,
                        from,
                        toExclusive
                );

        long numberOfDays =
                ChronoUnit.DAYS.between(
                        resolvedFrom,
                        resolvedTo
                ) + 1;

        LocalDate previousTo =
                resolvedFrom.minusDays(1);

        LocalDate previousFrom =
                previousTo.minusDays(numberOfDays - 1);

        Instant previousFromInstant =
                previousFrom
                        .atStartOfDay(GUATEMALA_ZONE)
                        .toInstant();

        Instant previousToExclusive =
                resolvedFrom
                        .atStartOfDay(GUATEMALA_ZONE)
                        .toInstant();

        BigDecimal previousProductsSold =
                saleItems.sumProductsSold(
                        SaleStatus.COMPLETED,
                        previousFromInstant,
                        previousToExclusive
                );

        BigDecimal changePercentage =
                calculatePercentageChange(
                        productsSold,
                        previousProductsSold
                );

        return new ProductsSoldReportResponse(
                resolvedFrom,
                resolvedTo,
                productsSold.stripTrailingZeros(),
                previousProductsSold.stripTrailingZeros(),
                changePercentage
        );
    }

    @Transactional(readOnly = true)
    public ResolvedAlertsReportResponse getResolvedAlertsReport(
            LocalDate fromDate,
            LocalDate toDate
    ) {
        LocalDate resolvedFrom = fromDate;
        LocalDate resolvedTo = toDate;

        LocalDate today = LocalDate.now(GUATEMALA_ZONE);

        if (resolvedFrom == null) {
            resolvedFrom = today.withDayOfMonth(1);
        }

        if (resolvedTo == null) {
            resolvedTo = today;
        }

        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la fecha final."
            );
        }

        Instant from = resolvedFrom
                .atStartOfDay(GUATEMALA_ZONE)
                .toInstant();

        Instant toExclusive = resolvedTo
                .plusDays(1)
                .atStartOfDay(GUATEMALA_ZONE)
                .toInstant();

        long resolvedAlerts =
                notifications.countResolvedAlerts(
                        from,
                        toExclusive
                );

        long numberOfDays =
                ChronoUnit.DAYS.between(
                        resolvedFrom,
                        resolvedTo
                ) + 1;

        LocalDate previousTo =
                resolvedFrom.minusDays(1);

        LocalDate previousFrom =
                previousTo.minusDays(numberOfDays - 1);

        Instant previousFromInstant =
                previousFrom
                        .atStartOfDay(GUATEMALA_ZONE)
                        .toInstant();

        Instant previousToExclusive =
                resolvedFrom
                        .atStartOfDay(GUATEMALA_ZONE)
                        .toInstant();

        long previousResolvedAlerts =
                notifications.countResolvedAlerts(
                        previousFromInstant,
                        previousToExclusive
                );

        BigDecimal changePercentage =
                calculatePercentageChange(
                        BigDecimal.valueOf(resolvedAlerts),
                        BigDecimal.valueOf(previousResolvedAlerts)
                );

        return new ResolvedAlertsReportResponse(
                resolvedFrom,
                resolvedTo,
                resolvedAlerts,
                previousResolvedAlerts,
                changePercentage
        );
    }

    @Transactional(readOnly = true)
    public DailySalesReportResponse getDailySalesReport(
            LocalDate fromDate,
            LocalDate toDate
    ) {
        LocalDate resolvedFrom = fromDate;
        LocalDate resolvedTo = toDate;

        LocalDate today = LocalDate.now(GUATEMALA_ZONE);

        if (resolvedFrom == null) {
            resolvedFrom = today.withDayOfMonth(1);
        }

        if (resolvedTo == null) {
            resolvedTo = today;
        }

        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la fecha final."
            );
        }

        Instant from = resolvedFrom
                .atStartOfDay(GUATEMALA_ZONE)
                .toInstant();

        Instant toExclusive = resolvedTo
                .plusDays(1)
                .atStartOfDay(GUATEMALA_ZONE)
                .toInstant();

        List<Sale> completedSales =
                sales.findByStatusAndSaleDateGreaterThanEqualAndSaleDateLessThanOrderBySaleDateAsc(
                        SaleStatus.COMPLETED,
                        from,
                        toExclusive
                );

        Map<LocalDate, DailyAccumulator> daily =
                new LinkedHashMap<>();

        LocalDate currentDate = resolvedFrom;

        while (!currentDate.isAfter(resolvedTo)) {
            daily.put(
                    currentDate,
                    new DailyAccumulator(
                            0,
                            BigDecimal.ZERO
                    )
            );

            currentDate = currentDate.plusDays(1);
        }

        for (Sale sale : completedSales) {
            LocalDate saleDate = sale
                    .getSaleDate()
                    .atZone(GUATEMALA_ZONE)
                    .toLocalDate();

            DailyAccumulator accumulator =
                    daily.get(saleDate);

            if (accumulator == null) {
                continue;
            }

            daily.put(
                    saleDate,
                    new DailyAccumulator(
                            accumulator.salesCount() + 1,
                            accumulator.totalSales()
                                    .add(sale.getTotalAmount())
                    )
            );
        }

        List<DailySalesPointResponse> points =
                new ArrayList<>();

        for (Map.Entry<LocalDate, DailyAccumulator> entry
                : daily.entrySet()) {

            points.add(
                    new DailySalesPointResponse(
                            entry.getKey(),
                            entry.getValue().salesCount(),
                            entry.getValue()
                                    .totalSales()
                                    .setScale(
                                            2,
                                            RoundingMode.HALF_UP
                                    )
                    )
            );
        }

        return new DailySalesReportResponse(
                resolvedFrom,
                resolvedTo,
                points
        );
    }

    private record DailyAccumulator(
            long salesCount,
            BigDecimal totalSales
    ) {
    }

    @Transactional(readOnly = true)
    public TopProductsReportResponse getTopProductsReport(
            LocalDate fromDate,
            LocalDate toDate
    ) {
        LocalDate resolvedFrom = fromDate;
        LocalDate resolvedTo = toDate;

        LocalDate today = LocalDate.now(GUATEMALA_ZONE);

        if (resolvedFrom == null) {
            resolvedFrom = today.withDayOfMonth(1);
        }

        if (resolvedTo == null) {
            resolvedTo = today;
        }

        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la fecha final."
            );
        }

        Instant from = resolvedFrom
                .atStartOfDay(GUATEMALA_ZONE)
                .toInstant();

        Instant toExclusive = resolvedTo
                .plusDays(1)
                .atStartOfDay(GUATEMALA_ZONE)
                .toInstant();

        List<TopProductProjection> results =
                saleItems.findTopProducts(
                        SaleStatus.COMPLETED,
                        from,
                        toExclusive,
                        PageRequest.of(0, 5)
                );

        List<TopProductResponse> products =
                results.stream()
                        .map(result ->
                                new TopProductResponse(
                                        result.getProductId(),
                                        result.getProductCode(),
                                        result.getProductName(),
                                        result.getQuantitySold()
                                                .stripTrailingZeros(),
                                        result.getTotalSales()
                                                .setScale(
                                                        2,
                                                        RoundingMode.HALF_UP
                                                )
                                )
                        )
                        .toList();

        return new TopProductsReportResponse(
                resolvedFrom,
                resolvedTo,
                products
        );
    }

    @Transactional(readOnly = true)
    public CategoryReportResponse getCategoryReport(
            LocalDate fromDate,
            LocalDate toDate
    ) {
        LocalDate resolvedFrom = fromDate;
        LocalDate resolvedTo = toDate;

        LocalDate today = LocalDate.now(GUATEMALA_ZONE);

        if (resolvedFrom == null) {
            resolvedFrom = today.withDayOfMonth(1);
        }

        if (resolvedTo == null) {
            resolvedTo = today;
        }

        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la fecha final."
            );
        }

        Instant from = resolvedFrom
                .atStartOfDay(GUATEMALA_ZONE)
                .toInstant();

        Instant toExclusive = resolvedTo
                .plusDays(1)
                .atStartOfDay(GUATEMALA_ZONE)
                .toInstant();

        List<CategoryReportProjection> results =
                saleItems.findCategorySummary(
                        SaleStatus.COMPLETED,
                        from,
                        toExclusive
                );

        BigDecimal totalSales = results.stream()
                .map(CategoryReportProjection::getTotalSales)
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );

        List<CategoryReportItemResponse> categories =
                results.stream()
                        .map(result -> {
                            BigDecimal percentage;

                            if (totalSales.compareTo(BigDecimal.ZERO) == 0) {
                                percentage =
                                        BigDecimal.ZERO.setScale(2);
                            } else {
                                percentage =
                                        result.getTotalSales()
                                                .divide(
                                                        totalSales,
                                                        6,
                                                        RoundingMode.HALF_UP
                                                )
                                                .multiply(
                                                        BigDecimal.valueOf(100)
                                                )
                                                .setScale(
                                                        2,
                                                        RoundingMode.HALF_UP
                                                );
                            }

                            return new CategoryReportItemResponse(
                                    result.getCategoryId(),
                                    result.getCategoryName(),
                                    result.getProductsSold()
                                            .stripTrailingZeros(),
                                    result.getTotalSales()
                                            .setScale(
                                                    2,
                                                    RoundingMode.HALF_UP
                                            ),
                                    result.getEstimatedMargin()
                                            .setScale(
                                                    2,
                                                    RoundingMode.HALF_UP
                                            ),
                                    percentage
                            );
                        })
                        .toList();

        return new CategoryReportResponse(
                resolvedFrom,
                resolvedTo,
                categories
        );
    }

    private BigDecimal calculatePercentageChange(
            BigDecimal current,
            BigDecimal previous
    ) {
        if (previous == null ||
                previous.compareTo(BigDecimal.ZERO) == 0) {

            if (current == null ||
                    current.compareTo(BigDecimal.ZERO) == 0) {
                return BigDecimal.ZERO.setScale(2);
            }

            return null;
        }

        return current
                .subtract(previous)
                .divide(
                        previous,
                        6,
                        RoundingMode.HALF_UP
                )
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }


}