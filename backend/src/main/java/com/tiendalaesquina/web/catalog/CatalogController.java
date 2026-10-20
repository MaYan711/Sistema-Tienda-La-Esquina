package com.tiendalaesquina.web.catalog;

import com.tiendalaesquina.application.catalog.CatalogService;
import com.tiendalaesquina.web.dto.ActiveStatusRequest;
import com.tiendalaesquina.web.dto.CategoryRequest;
import com.tiendalaesquina.web.dto.CategoryResponse;
import com.tiendalaesquina.web.dto.MeasurementUnitRequest;
import com.tiendalaesquina.web.dto.MeasurementUnitResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping("/catalog")
@Tag(name = "Catálogo", description = "Categorías y unidades de medida")
@SecurityRequirement(name = "bearerAuth")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Listar categorías")
    public List<CategoryResponse> categories(
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        return catalog.categories(activeOnly);
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear una categoría")
    public CategoryResponse createCategory(Authentication authentication,
                                           @Valid @RequestBody CategoryRequest request) {
        return catalog.createCategory(request, authentication.getName());
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Editar una categoría")
    public CategoryResponse updateCategory(Authentication authentication,
                                           @PathVariable Long id,
                                           @Valid @RequestBody CategoryRequest request) {
        return catalog.updateCategory(id, request, authentication.getName());
    }

    @PatchMapping("/categories/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activar o desactivar una categoría")
    public CategoryResponse setCategoryStatus(Authentication authentication,
                                              @PathVariable Long id,
                                              @Valid @RequestBody ActiveStatusRequest request) {
        return catalog.setCategoryActive(id, request.active(), authentication.getName());
    }

    @GetMapping("/measurement-units")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    @Operation(summary = "Listar unidades de medida")
    public List<MeasurementUnitResponse> units(
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        return catalog.units(activeOnly);
    }

    @PostMapping("/measurement-units")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear una unidad de medida")
    public MeasurementUnitResponse createUnit(@Valid @RequestBody MeasurementUnitRequest request) {
        return catalog.createUnit(request);
    }

    @PutMapping("/measurement-units/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Editar una unidad de medida")
    public MeasurementUnitResponse updateUnit(@PathVariable Long id,
                                              @Valid @RequestBody MeasurementUnitRequest request) {
        return catalog.updateUnit(id, request);
    }

    @PatchMapping("/measurement-units/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activar o desactivar una unidad de medida")
    public MeasurementUnitResponse setUnitStatus(@PathVariable Long id,
                                                 @Valid @RequestBody ActiveStatusRequest request) {
        return catalog.setUnitActive(id, request.active());
    }
}
