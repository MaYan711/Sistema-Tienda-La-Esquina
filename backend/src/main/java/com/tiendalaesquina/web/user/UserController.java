package com.tiendalaesquina.web.user;

import com.tiendalaesquina.application.user.UserService;
import com.tiendalaesquina.web.dto.CreateUserRequest;
import com.tiendalaesquina.web.dto.UpdateUserRequest;
import com.tiendalaesquina.web.dto.UserResponse;
import com.tiendalaesquina.web.dto.UserStatusRequest;
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
@RequestMapping("/users")
@Tag(name = "Usuarios", description = "Administración de usuarios y roles")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "id", "email", "createdAt", "updatedAt", "role", "enabled"
    );

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Listar y filtrar usuarios",
        description = "Permite buscar por email y filtrar por rol y estado habilitado, con paginación y ordenamiento")
    public Page<UserResponse> list(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) Boolean enabled,
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

        return userService.search(search, role, enabled, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar usuario por ID")
    public UserResponse getById(@PathVariable Long id) {
        return userService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear nuevo usuario")
    public UserResponse create(Authentication authentication,
                               @Valid @RequestBody CreateUserRequest request) {
        return userService.create(request, authentication.getName());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar datos de un usuario (email y rol)")
    public UserResponse update(Authentication authentication,
                               @PathVariable Long id,
                               @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request, authentication.getName());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activar o desactivar usuario")
    public UserResponse setStatus(Authentication authentication,
                                  @PathVariable Long id,
                                  @Valid @RequestBody UserStatusRequest request) {
        return userService.setStatus(id, request.enabled(), authentication.getName());
    }
}
