package com.tiendalaesquina.web.product;

import com.tiendalaesquina.application.product.ProductService;
import com.tiendalaesquina.web.dto.ActiveStatusRequest;
import com.tiendalaesquina.web.dto.CreateProductRequest;
import com.tiendalaesquina.web.dto.ProductResponse;
import com.tiendalaesquina.web.dto.UpdateProductRequest;
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
@RequestMapping("/products")
@Tag(name = "Productos", description = "Catálogo, precios, existencias y estado de productos")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "code", "name", "purchasePrice", "salePrice",
            "currentStock", "minimumStock", "createdAt", "updatedAt"
    );

    private final ProductService products;

    public ProductController(ProductService products) {
        this.products = products;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Listar y filtrar productos",
            description = "Permite buscar por código o nombre y filtrar por categoría, estado activo y estado de inventario")
    public Page<ProductResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String stockStatus,
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

        return products.search(search, categoryId, active, stockStatus, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Consultar un producto")
    public ProductResponse get(@PathVariable Long id) {
        return products.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear un producto")
    public ProductResponse create(Authentication authentication,
                                  @Valid @RequestBody CreateProductRequest request) {
        return products.create(request, authentication.getName());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Editar datos de un producto",
            description = "No modifica la existencia actual. El stock se ajustará mediante el módulo de inventario")
    public ProductResponse update(Authentication authentication,
                                  @PathVariable Long id,
                                  @Valid @RequestBody UpdateProductRequest request) {
        return products.update(id, request, authentication.getName());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activar o desactivar un producto")
    public ProductResponse setStatus(Authentication authentication,
                                     @PathVariable Long id,
                                     @Valid @RequestBody ActiveStatusRequest request) {
        return products.setActive(id, request.active(), authentication.getName());
    }
}