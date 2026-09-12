package com.tiendalaesquina.web.stockentry;

import com.tiendalaesquina.application.stockentry.StockEntryService;
import com.tiendalaesquina.web.dto.CreateStockEntryRequest;
import com.tiendalaesquina.web.dto.StockEntryResponse;
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
@RequestMapping("/stock-entries")
@Tag(
        name = "Entradas de mercaderia",
        description = "Registro e historial de entradas de mercaderia"
)
@SecurityRequirement(name = "bearerAuth")
public class StockEntryController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "entryDate",
            "documentNumber",
            "status",
            "createdAt",
            "updatedAt"
    );

    private final StockEntryService stockEntries;

    public StockEntryController(StockEntryService stockEntries) {
        this.stockEntries = stockEntries;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar una entrada de mercaderia")
    public StockEntryResponse create(
            Authentication authentication,
            @Valid @RequestBody CreateStockEntryRequest request
    ) {
        return stockEntries.create(
                request,
                authentication.getName()
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Listar entradas de mercaderia",
            description = "Permite filtrar por proveedor, rango de fechas y numero de documento"
    )
    public Page<StockEntryResponse> search(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String reference,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "entryDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 || size > 100 ? 10 : size;

        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy)
                ? sortBy
                : "entryDate";

        Sort.Direction safeDirection =
                "asc".equalsIgnoreCase(direction)
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(safeDirection, safeSortBy)
        );

        return stockEntries.search(
                supplierId,
                fromDate,
                toDate,
                reference,
                pageable
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Consultar una entrada de mercaderia")
    public StockEntryResponse get(@PathVariable Long id) {
        return stockEntries.get(id);
    }
}