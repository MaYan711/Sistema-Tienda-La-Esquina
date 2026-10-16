package com.tiendalaesquina.web.schedule;

import com.tiendalaesquina.application.schedule.OperatingHoursManagementService;
import com.tiendalaesquina.web.dto.OperatingHourExceptionRequest;
import com.tiendalaesquina.web.dto.OperatingHourExceptionResponse;
import com.tiendalaesquina.web.dto.OperatingHoursConfigurationResponse;
import com.tiendalaesquina.web.dto.UpdateOperatingHoursRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/operating-hours")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Horario operativo", description = "Administración del horario semanal y excepciones de ventas")
@SecurityRequirement(name = "bearerAuth")
public class OperatingHoursController {

    private final OperatingHoursManagementService operatingHours;

    public OperatingHoursController(OperatingHoursManagementService operatingHours) {
        this.operatingHours = operatingHours;
    }

    @GetMapping("/configuration")
    @Operation(summary = "Consultar configuración del horario operativo")
    public OperatingHoursConfigurationResponse configuration() {
        return operatingHours.getConfiguration();
    }

    @PutMapping("/weekly")
    @Operation(summary = "Actualizar horario semanal de empleados")
    public OperatingHoursConfigurationResponse updateWeekly(
            Authentication authentication,
            @Valid @RequestBody UpdateOperatingHoursRequest request
    ) {
        return operatingHours.updateWeekly(request, authentication.getName());
    }

    @PostMapping("/exceptions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear una excepción de horario para una fecha")
    public OperatingHourExceptionResponse createException(
            Authentication authentication,
            @Valid @RequestBody OperatingHourExceptionRequest request
    ) {
        return operatingHours.createException(request, authentication.getName());
    }

    @PutMapping("/exceptions/{id}")
    @Operation(summary = "Actualizar una excepción de horario")
    public OperatingHourExceptionResponse updateException(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody OperatingHourExceptionRequest request
    ) {
        return operatingHours.updateException(id, request, authentication.getName());
    }

    @DeleteMapping("/exceptions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar una excepción de horario")
    public void deleteException(@PathVariable Long id) {
        operatingHours.deleteException(id);
    }
}
