
package com.tiendalaesquina.application.schedule;

import com.tiendalaesquina.config.OperatingHoursProperties;
import com.tiendalaesquina.domain.model.EmployeeOperatingHour;
import com.tiendalaesquina.domain.model.EmployeeOperatingHourException;
import com.tiendalaesquina.domain.model.RoleName;
import com.tiendalaesquina.domain.model.UserAccount;
import com.tiendalaesquina.domain.repository.EmployeeOperatingHourExceptionRepository;
import com.tiendalaesquina.domain.repository.EmployeeOperatingHourRepository;
import com.tiendalaesquina.domain.repository.UserAccountRepository;
import com.tiendalaesquina.exception.ApiException;
import com.tiendalaesquina.web.dto.OperatingHoursResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
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
    private final EmployeeOperatingHourRepository schedules;
    private final EmployeeOperatingHourExceptionRepository exceptions;
    private final Clock clock;

    public EmployeeOperatingHoursService(
            OperatingHoursProperties properties,
            UserAccountRepository users,
            EmployeeOperatingHourRepository schedules,
            EmployeeOperatingHourExceptionRepository exceptions,
            Clock clock
    ) {
        this.properties = properties;
        this.users = users;
        this.schedules = schedules;
        this.exceptions = exceptions;
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
        LocalDate currentDate = now.toLocalDate();
        LocalTime currentTime = now.toLocalTime();
        EffectiveSchedule effective = resolveEffectiveSchedule(currentDate);

        RoleName role = actor.getRole().getName();
        boolean employeeRestricted = role == RoleName.EMPLOYEE && properties.isEnabled();
        boolean insideSchedule = !effective.closed()
                && isInsideSchedule(currentTime, effective.openingTime(), effective.closingTime());
        boolean operationAllowed = !employeeRestricted || insideSchedule;

        String message = buildMessage(
                role,
                currentTime,
                effective,
                operationAllowed
        );

        return new OperatingHoursResponse(
                properties.isEnabled(),
                zoneId.getId(),
                effective.closed() ? null : formatTime(effective.openingTime()),
                effective.closed() ? null : formatTime(effective.closingTime()),
                currentDate.format(DATE_FORMAT),
                formatTime(currentTime),
                role.name(),
                employeeRestricted,
                operationAllowed,
                effective.closed(),
                effective.source(),
                effective.reason(),
                message
        );
    }

    private EffectiveSchedule resolveEffectiveSchedule(LocalDate date) {
        EmployeeOperatingHourException exception = exceptions.findByExceptionDate(date).orElse(null);
        if (exception != null) {
            if (exception.isClosed()) {
                return new EffectiveSchedule(
                        true,
                        null,
                        null,
                        "EXCEPTION",
                        exception.getReason()
                );
            }

            return new EffectiveSchedule(
                    false,
                    exception.getOpeningTime(),
                    exception.getClosingTime(),
                    "EXCEPTION",
                    exception.getReason()
            );
        }

        EmployeeOperatingHour weekly = schedules.findByDayOfWeek(date.getDayOfWeek()).orElse(null);
        if (weekly != null) {
            if (!weekly.isEnabled()) {
                return new EffectiveSchedule(
                        true,
                        null,
                        null,
                        "WEEKLY",
                        "Día cerrado según horario semanal"
                );
            }

            return new EffectiveSchedule(
                    false,
                    weekly.getOpeningTime(),
                    weekly.getClosingTime(),
                    "WEEKLY",
                    "Horario semanal configurado"
            );
        }

        return new EffectiveSchedule(
                false,
                properties.getOpeningTime(),
                properties.getClosingTime(),
                "DEFAULT",
                "Horario predeterminado"
        );
    }

    private String buildMessage(
            RoleName role,
            LocalTime currentTime,
            EffectiveSchedule schedule,
            boolean operationAllowed
    ) {
        if (!properties.isEnabled()) {
            return "La restricción de horario se encuentra desactivada.";
        }

        if (role == RoleName.ADMIN) {
            if (schedule.closed()) {
                return "Hoy existe un cierre configurado para empleados"
                        + reasonSuffix(schedule.reason())
                        + ". La cuenta administradora puede realizar operaciones extraordinarias.";
            }
            return "El horario efectivo para empleados hoy es de "
                    + formatTime(schedule.openingTime())
                    + " a "
                    + formatTime(schedule.closingTime())
                    + reasonSuffix(schedule.reason())
                    + ". La cuenta administradora puede operar fuera de este horario.";
        }

        if (schedule.closed()) {
            return "La tienda está cerrada para operaciones de empleados hoy"
                    + reasonSuffix(schedule.reason())
                    + ". Solicita apoyo de un administrador si se requiere una operación extraordinaria.";
        }

        if (operationAllowed) {
            return "Turno habilitado. El horario efectivo de hoy es de "
                    + formatTime(schedule.openingTime())
                    + " a "
                    + formatTime(schedule.closingTime())
                    + reasonSuffix(schedule.reason())
                    + ".";
        }

        return "No se pueden registrar ventas fuera del horario autorizado para empleados ("
                + formatTime(schedule.openingTime())
                + " - "
                + formatTime(schedule.closingTime())
                + ", hora de Guatemala). Hora actual: "
                + formatTime(currentTime)
                + reasonSuffix(schedule.reason())
                + ". Solicita apoyo de un administrador si se requiere una operación extraordinaria.";
    }

    private String reasonSuffix(String reason) {
        if (reason == null || reason.isBlank()) {
            return "";
        }
        return " · " + reason;
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

    private record EffectiveSchedule(
            boolean closed,
            LocalTime openingTime,
            LocalTime closingTime,
            String source,
            String reason
    ) {
    }
}
