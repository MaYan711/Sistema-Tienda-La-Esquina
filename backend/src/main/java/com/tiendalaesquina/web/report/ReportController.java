package com.tiendalaesquina.web.report;

import com.tiendalaesquina.application.report.ReportService;
import com.tiendalaesquina.web.dto.SalesReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tiendalaesquina.web.dto.MarginReportResponse;
import com.tiendalaesquina.web.dto.ProductsSoldReportResponse;
import com.tiendalaesquina.web.dto.ResolvedAlertsReportResponse;
import com.tiendalaesquina.web.dto.DailySalesReportResponse;
import com.tiendalaesquina.web.dto.TopProductsReportResponse;
import com.tiendalaesquina.web.dto.CategoryReportResponse;

import java.time.LocalDate;

@RestController
@RequestMapping("/reports")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reportes", description = "Reportes y análisis de la tienda")
public class ReportController {

    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/sales")
    @Operation(
            summary = "Reporte de ventas por período",
            description = "Calcula las ventas completadas dentro del rango seleccionado"
    )


    public SalesReportResponse sales(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        return reports.getSalesReport(
                fromDate,
                toDate
        );
    }

    @GetMapping("/margin")
    @Operation(
            summary = "Reporte de margen estimado",
            description = "Calcula el margen de las ventas completadas usando el costo histórico de cada producto"
    )
    public MarginReportResponse margin(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        return reports.getMarginReport(
                fromDate,
                toDate
        );
    }

    @GetMapping("/products-sold")
    @Operation(
            summary = "Reporte de productos vendidos",
            description = "Calcula la cantidad total de productos vendidos dentro del período seleccionado"
    )
    public ProductsSoldReportResponse productsSold(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        return reports.getProductsSoldReport(
                fromDate,
                toDate
        );
    }

    @GetMapping("/resolved-alerts")
    @Operation(
            summary = "Reporte de alertas resueltas",
            description = "Cuenta las alertas de inventario atendidas dentro del período seleccionado"
    )
    public ResolvedAlertsReportResponse resolvedAlerts(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        return reports.getResolvedAlertsReport(
                fromDate,
                toDate
        );
    }

    @GetMapping("/daily-sales")
    @Operation(
            summary = "Reporte de ventas por día",
            description = "Agrupa las ventas completadas por día dentro del período seleccionado"
    )
    public DailySalesReportResponse dailySales(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        return reports.getDailySalesReport(
                fromDate,
                toDate
        );
    }

    @GetMapping("/top-products")
    @Operation(
            summary = "Reporte de productos más vendidos",
            description = "Obtiene los cinco productos con mayor cantidad vendida dentro del período seleccionado"
    )
    public TopProductsReportResponse topProducts(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        return reports.getTopProductsReport(
                fromDate,
                toDate
        );
    }

    @GetMapping("/categories")
    @Operation(
            summary = "Reporte por categoría",
            description = "Agrupa productos vendidos, ventas y margen estimado por categoría"
    )
    public CategoryReportResponse categories(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate
    ) {
        return reports.getCategoryReport(
                fromDate,
                toDate
        );
    }
}