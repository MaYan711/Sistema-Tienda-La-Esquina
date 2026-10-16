package com.tiendalaesquina.application.schedule;

import com.tiendalaesquina.config.OperatingHoursProperties;
import com.tiendalaesquina.domain.model.RoleName;
import com.tiendalaesquina.domain.model.UserAccount;
import com.tiendalaesquina.domain.repository.UserAccountRepository;
import com.tiendalaesquina.exception.ApiException;
import com.tiendalaesquina.web.dto.OperatingHoursResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmployeeOperatingHoursService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final OperatingHoursProperties properties;
    private final UserAccountRepository users;
    private final Clock clock;

    public EmployeeOperatingHoursService(
            OperatingHoursProperties properties,
            UserAccountRepository users,
            Clock clock
    ) {
        this.properties = properties;
        this.users = users;
        this.clock = clock;
    }

    public OperatingHoursResponse getStatus(String actorEmail) {
        UserAccount actor = requireActor(actorEmail);
        return buildStatus(actor);
    }

    public void assertCanRegisterSale(UserAccount actor) {
        OperatingHoursResponse status = buildStatus(actor);

        if (!status.operationAllowed()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "outside_operating_hours",
                    "Fuera de horario operativo",
                    status.message()
            );
        }
    }

    private OperatingHoursResponse buildStatus(UserAccount actor) {
        ZoneId zoneId = resolveZoneId();
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(zoneId);
        LocalTime currentTime = now.toLocalTime();
        LocalTime openingTime = properties.getOpeningTime();
        LocalTime closingTime = properties.getClosingTime();

        RoleName role = actor.getRole().getName();
        boolean employeeRestricted = role == RoleName.EMPLOYEE && properties.isEnabled();
        boolean insideSchedule = isInsideSchedule(currentTime, openingTime, closingTime);
        boolean operationAllowed = !employeeRestricted || insideSchedule;

        String message;

        if (!properties.isEnabled()) {
            message = "La restricción de horario se encuentra desactivada.";
        } else if (role == RoleName.ADMIN) {
            message = "El horario para empleados es de "
                    + formatTime(openingTime)
                    + " a "
                    + formatTime(closingTime)
                    + ". La cuenta administradora puede realizar operaciones fuera de este horario.";
        } else if (operationAllowed) {
            message = "Turno habilitado. Los empleados pueden registrar ventas de "
                    + formatTime(openingTime)
                    + " a "
                    + formatTime(closingTime)
                    + ".";
        } else {
            message = "No se pueden registrar ventas fuera del horario autorizado para empleados ("
                    + formatTime(openingTime)
                    + " - "
                    + formatTime(closingTime)
                    + ", hora de Guatemala). Hora actual: "
                    + formatTime(currentTime)
                    + ". Solicita apoyo de un administrador si se requiere una operación extraordinaria.";
        }

        return new OperatingHoursResponse(
                properties.isEnabled(),
                zoneId.getId(),
                formatTime(openingTime),
                formatTime(closingTime),
                now.toLocalDate().format(DATE_FORMAT),
                formatTime(currentTime),
                role.name(),
                employeeRestricted,
                operationAllowed,
                message
        );
    }

    private boolean isInsideSchedule(
            LocalTime currentTime,
            LocalTime openingTime,
            LocalTime closingTime
    ) {
        if (openingTime.equals(closingTime)) {
            return true;
        }

        if (openingTime.isBefore(closingTime)) {
            return !currentTime.isBefore(openingTime)
                    && currentTime.isBefore(closingTime);
        }

        return !currentTime.isBefore(openingTime)
                || currentTime.isBefore(closingTime);
    }

    private UserAccount requireActor(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "account_not_found",
                        "Cuenta no encontrada",
                        "No fue posible identificar al usuario autenticado"
                ));
    }

    private ZoneId resolveZoneId() {
        try {
            return ZoneId.of(properties.getTimeZone());
        } catch (Exception exception) {
            return ZoneId.of("America/Guatemala");
        }
    }

    private String formatTime(LocalTime time) {
        return time.format(TIME_FORMAT);
    }
}
