package com.tiendalaesquina.web.inventory;

import com.tiendalaesquina.application.inventory.InventoryService;
import com.tiendalaesquina.web.dto.InventoryAdjustmentRequest;
import com.tiendalaesquina.web.dto.InventoryAdjustmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory")
@Tag(name = "Inventario", description = "Operaciones, movimientos y ajustes de existencias")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
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
