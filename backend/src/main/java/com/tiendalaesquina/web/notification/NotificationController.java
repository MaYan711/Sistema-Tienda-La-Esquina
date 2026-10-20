package com.tiendalaesquina.web.notification;

import com.tiendalaesquina.application.inventory.StockNotificationService;
import com.tiendalaesquina.web.dto.StockNotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notificaciones", description = "Centro de notificaciones y alertas de inventario")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "notificationType", "title", "isRead", "readAt", "createdAt"
    );

    private final StockNotificationService notificationService;

    public NotificationController(StockNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Consultar notificaciones y alertas",
            description = "Permite a un usuario con rol ADMIN consultar las notificaciones de inventario con filtros opcionales por producto, tipo y estado de lectura, además de paginación y ordenamiento"
    )
    public Page<StockNotificationResponse> search(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean isRead,
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

        return notificationService.searchNotifications(productId, type, isRead, pageable);
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Marcar notificación como leída",
            description = "Permite a un usuario con rol ADMIN marcar una notificación como leída. Si ya fue leída, devuelve el registro actual sin modificarlo de forma idempotente"
    )
    public StockNotificationResponse markAsRead(Authentication authentication,
                                                @PathVariable Long id) {
        return notificationService.markAsRead(id, authentication.getName());
    }
}
