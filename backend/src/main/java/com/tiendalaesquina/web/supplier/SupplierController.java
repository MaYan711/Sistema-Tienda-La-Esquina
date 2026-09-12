package com.tiendalaesquina.web.supplier;

import com.tiendalaesquina.application.supplier.SupplierService;
import com.tiendalaesquina.web.dto.ActiveStatusRequest;
import com.tiendalaesquina.web.dto.CreateSupplierRequest;
import com.tiendalaesquina.web.dto.SupplierResponse;
import com.tiendalaesquina.web.dto.UpdateSupplierRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/suppliers")
@Tag(name = "Proveedores", description = "Administracion de proveedores")
@SecurityRequirement(name = "bearerAuth")
public class SupplierController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "name",
            "nit",
            "phone",
            "email",
            "active",
            "createdAt",
            "updatedAt"
    );

    private final SupplierService suppliers;

    public SupplierController(SupplierService suppliers) {
        this.suppliers = suppliers;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Listar y filtrar proveedores",
            description = "Permite buscar por nombre, NIT, telefono o correo"
    )
    public Page<SupplierResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 || size > 100 ? 10 : size;
        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "name";

        Sort.Direction safeDirection = "desc".equalsIgnoreCase(direction)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(safeDirection, safeSortBy)
        );

        return suppliers.search(search, active, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Consultar un proveedor")
    public SupplierResponse get(@PathVariable Long id) {
        return suppliers.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear un proveedor")
    public SupplierResponse create(
            Authentication authentication,
            @Valid @RequestBody CreateSupplierRequest request
    ) {
        return suppliers.create(request, authentication.getName());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Editar un proveedor")
    public SupplierResponse update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateSupplierRequest request
    ) {
        return suppliers.update(id, request, authentication.getName());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activar o desactivar un proveedor")
    public SupplierResponse setStatus(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody ActiveStatusRequest request
    ) {
        return suppliers.setActive(
                id,
                request.active(),
                authentication.getName()
        );
    }
}