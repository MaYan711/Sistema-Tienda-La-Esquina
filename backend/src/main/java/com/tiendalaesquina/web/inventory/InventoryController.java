package com.tiendalaesquina.web.inventory;

import com.tiendalaesquina.application.inventory.InventoryService;
import com.tiendalaesquina.web.dto.InventoryAdjustmentRequest;
import com.tiendalaesquina.web.dto.InventoryAdjustmentResponse;
import com.tiendalaesquina.web.dto.InventoryMovementResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/inventory")
@Tag(name = "Inventario", description = "Operaciones, movimientos y ajustes de existencias")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "movementType", "quantityDelta", "quantityBefore",
            "quantityAfter", "sourceType", "sourceId", "createdAt"
    );

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/movements")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Consultar movimientos de inventario",
            description = "Permite a un usuario con rol ADMIN consultar el historial de movimientos de inventario con filtros opcionales por producto y tipo de movimiento, además de paginación y ordenamiento"
    )
    public Page<InventoryMovementResponse> searchMovements(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String movementType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 || size > 100 ? 10 : size;
        String safeSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction safeDirection = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(safeDirection, safeSortBy)
        );

        return inventoryService.searchMovements(productId, movementType, pageable);
    }

    @PostMapping("/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Realizar ajuste de inventario",
            description = "Permite a un usuario con rol ADMIN ajustar las existencias de un producto de forma transaccional, registrando el ajuste, el movimiento de inventario, auditoría y alertas de stock correspondientes"
    )
    public InventoryAdjustmentResponse adjustStock(Authentication authentication,
                                                  @Valid @RequestBody InventoryAdjustmentRequest request) {
        return inventoryService.adjustStock(request, authentication.getName());
    }
}
