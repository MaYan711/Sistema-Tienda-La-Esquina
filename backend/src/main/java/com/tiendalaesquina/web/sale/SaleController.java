package com.tiendalaesquina.web.sale;

import com.tiendalaesquina.application.sale.SaleService;
import com.tiendalaesquina.application.schedule.EmployeeOperatingHoursService;
import com.tiendalaesquina.web.dto.CreateSaleRequest;
import com.tiendalaesquina.web.dto.OperatingHoursResponse;
import com.tiendalaesquina.web.dto.SaleDetailResponse;
import com.tiendalaesquina.web.dto.SaleReceiptResponse;
import com.tiendalaesquina.web.dto.SaleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Set;

@RestController
@RequestMapping("/sales")
@Tag(
        name = "Ventas",
        description = "Registro e historial de ventas en efectivo"
)
@SecurityRequirement(name = "bearerAuth")
public class SaleController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "saleNumber",
            "saleDate",
            "totalAmount",
            "status",
            "createdAt"
    );

    private final SaleService sales;
    private final EmployeeOperatingHoursService operatingHours;

    public SaleController(
            SaleService sales,
            EmployeeOperatingHoursService operatingHours
    ) {
        this.sales = sales;
        this.operatingHours = operatingHours;
    }


    @GetMapping("/operating-hours")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(
            summary = "Consultar horario operativo de ventas",
            description = "Indica el horario autorizado para empleados y si el usuario actual puede registrar una venta en este momento"
    )
    public OperatingHoursResponse operatingHours(Authentication authentication) {
        return operatingHours.getStatus(authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Registrar una venta en efectivo")
    public SaleResponse create(
            Authentication authentication,
            @Valid @RequestBody CreateSaleRequest request
    ) {
        return sales.create(
                request,
                authentication.getName()
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Listar ventas")
    public Page<SaleResponse> search(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String reference,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "saleDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 || size > 100 ? 10 : size;

        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy)
                ? sortBy
                : "saleDate";

        Sort.Direction safeDirection =
                "asc".equalsIgnoreCase(direction)
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(safeDirection, safeSortBy)
        );

        return sales.search(
                fromDate,
                toDate,
                reference,
                pageable
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Consultar una venta")
    public SaleResponse get(@PathVariable Long id) {
        return sales.get(id);
    }

    @GetMapping("/{id}/detail")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(
            summary = "Consultar el detalle completo de una venta",
            description = "Devuelve los productos vendidos, cantidades, precios, totales, pago, cambio, usuario y observaciones de la venta"
    )
    public SaleDetailResponse detail(@PathVariable Long id) {
        return sales.getDetail(id);
    }

    @GetMapping("/{id}/receipt")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    @Operation(
            summary = "Obtener comprobante de una venta",
            description = "Devuelve los datos necesarios para mostrar, imprimir o descargar el comprobante de compra"
    )
    public SaleReceiptResponse receipt(@PathVariable Long id) {
        return sales.getReceipt(id);
    }
}