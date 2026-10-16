package com.tiendalaesquina.application.schedule;

import com.tiendalaesquina.config.OperatingHoursProperties;
import com.tiendalaesquina.domain.model.EmployeeOperatingHour;
import com.tiendalaesquina.domain.model.EmployeeOperatingHourException;
import com.tiendalaesquina.domain.model.UserAccount;
import com.tiendalaesquina.domain.repository.EmployeeOperatingHourExceptionRepository;
import com.tiendalaesquina.domain.repository.EmployeeOperatingHourRepository;
import com.tiendalaesquina.domain.repository.UserAccountRepository;
import com.tiendalaesquina.exception.ApiException;
import com.tiendalaesquina.web.dto.OperatingHourDayRequest;
import com.tiendalaesquina.web.dto.OperatingHourDayResponse;
import com.tiendalaesquina.web.dto.OperatingHourExceptionRequest;
import com.tiendalaesquina.web.dto.OperatingHourExceptionResponse;
import com.tiendalaesquina.web.dto.OperatingHoursConfigurationResponse;
import com.tiendalaesquina.web.dto.UpdateOperatingHoursRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class OperatingHoursManagementService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final OperatingHoursProperties properties;
    private final EmployeeOperatingHourRepository schedules;
    private final EmployeeOperatingHourExceptionRepository exceptions;
    private final UserAccountRepository users;

    public OperatingHoursManagementService(
            OperatingHoursProperties properties,
            EmployeeOperatingHourRepository schedules,
            EmployeeOperatingHourExceptionRepository exceptions,
            UserAccountRepository users
    ) {
        this.properties = properties;
        this.schedules = schedules;
        this.exceptions = exceptions;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public OperatingHoursConfigurationResponse getConfiguration() {
        Map<DayOfWeek, EmployeeOperatingHour> configured = new EnumMap<>(DayOfWeek.class);
        schedules.findAll().forEach(item -> configured.put(item.getDayOfWeek(), item));

        List<OperatingHourDayResponse> weekly = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            EmployeeOperatingHour item = configured.get(day);
            weekly.add(toDayResponse(day, item));
        }

        List<OperatingHourExceptionResponse> exceptionResponses = exceptions
                .findAllByOrderByExceptionDateAsc()
                .stream()
                .map(this::toExceptionResponse)
                .toList();

        return new OperatingHoursConfigurationResponse(
                properties.isEnabled(),
                properties.getTimeZone(),
                formatTime(properties.getOpeningTime()),
                formatTime(properties.getClosingTime()),
                weekly,
                exceptionResponses
        );
    }

    @Transactional
    public OperatingHoursConfigurationResponse updateWeekly(
            UpdateOperatingHoursRequest request,
            String actorEmail
    ) {
        UserAccount actor = requireActor(actorEmail);
        Map<DayOfWeek, OperatingHourDayRequest> requested = normalizeDays(request.days());

        for (DayOfWeek day : DayOfWeek.values()) {
            OperatingHourDayRequest dayRequest = requested.get(day);
            EmployeeOperatingHour schedule = schedules.findByDayOfWeek(day)
                    .orElseGet(() -> new EmployeeOperatingHour(
                            day,
                            dayRequest.enabled(),
                            dayRequest.openingTime(),
                            dayRequest.closingTime(),
                            actor
                    ));

            if (schedule.getId() != null) {
                schedule.update(
                        dayRequest.enabled(),
                        dayRequest.openingTime(),
                        dayRequest.closingTime(),
                        actor
                );
            }

            schedules.save(schedule);
        }

        return getConfiguration();
    }

    @Transactional
    public OperatingHourExceptionResponse createException(
            OperatingHourExceptionRequest request,
            String actorEmail
    ) {
        validateException(request);

        if (exceptions.existsByExceptionDate(request.date())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "operating_hour_exception_exists",
                    "Excepción duplicada",
                    "Ya existe una excepción de horario para la fecha " + request.date()
            );
        }

        UserAccount actor = requireActor(actorEmail);
        EmployeeOperatingHourException exception = new EmployeeOperatingHourException(
                request.date(),
                request.closed(),
                request.closed() ? null : request.openingTime(),
                request.closed() ? null : request.closingTime(),
                normalizeReason(request.reason()),
                actor
        );

        return toExceptionResponse(exceptions.save(exception));
    }

    @Transactional
    public OperatingHourExceptionResponse updateException(
            Long id,
            OperatingHourExceptionRequest request,
            String actorEmail
    ) {
        validateException(request);

        EmployeeOperatingHourException exception = exceptions.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "operating_hour_exception_not_found",
                        "Excepción no encontrada",
                        "La excepción de horario solicitada no existe"
                ));

        if (exceptions.existsByExceptionDateAndIdNot(request.date(), id)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "operating_hour_exception_exists",
                    "Excepción duplicada",
                    "Ya existe una excepción de horario para la fecha " + request.date()
            );
        }

        UserAccount actor = requireActor(actorEmail);
        exception.update(
                request.date(),
                request.closed(),
                request.closed() ? null : request.openingTime(),
                request.closed() ? null : request.closingTime(),
                normalizeReason(request.reason()),
                actor
        );

        return toExceptionResponse(exceptions.save(exception));
    }

    @Transactional
    public void deleteException(Long id) {
        EmployeeOperatingHourException exception = exceptions.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "operating_hour_exception_not_found",
                        "Excepción no encontrada",
                        "La excepción de horario solicitada no existe"
                ));
        exceptions.delete(exception);
    }

    private Map<DayOfWeek, OperatingHourDayRequest> normalizeDays(List<OperatingHourDayRequest> days) {
        if (days == null || days.size() != 7) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_operating_hours_week",
                    "Horario semanal incompleto",
                    "Debes enviar la configuración de los siete días de la semana"
            );
        }

        Map<DayOfWeek, OperatingHourDayRequest> result = new EnumMap<>(DayOfWeek.class);
        Set<DayOfWeek> seen = new HashSet<>();

        for (OperatingHourDayRequest item : days) {
            DayOfWeek day = parseDay(item.dayOfWeek());
            if (!seen.add(day)) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "duplicate_operating_hours_day",
                        "Día duplicado",
                        "La configuración semanal contiene el día " + day.name() + " más de una vez"
                );
            }
            result.put(day, item);
        }

        if (result.size() != 7) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_operating_hours_week",
                    "Horario semanal incompleto",
                    "Debes configurar los siete días de la semana"
            );
        }

        return result;
    }

    private DayOfWeek parseDay(String value) {
        try {
            return DayOfWeek.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_day_of_week",
                    "Día inválido",
                    "El día de la semana enviado no es válido"
            );
        }
    }

    private void validateException(OperatingHourExceptionRequest request) {
        if (!request.closed() && (request.openingTime() == null || request.closingTime() == null)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "operating_hour_exception_times_required",
                    "Horario especial incompleto",
                    "Debes indicar hora de apertura y cierre cuando la tienda no está marcada como cerrada"
            );
        }
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

    private OperatingHourDayResponse toDayResponse(
            DayOfWeek day,
            EmployeeOperatingHour configured
    ) {
        if (configured == null) {
            return new OperatingHourDayResponse(
                    day.name(),
                    dayLabel(day),
                    true,
                    formatTime(properties.getOpeningTime()),
                    formatTime(properties.getClosingTime()),
                    false
            );
        }

        return new OperatingHourDayResponse(
                day.name(),
                dayLabel(day),
                configured.isEnabled(),
                formatTime(configured.getOpeningTime()),
                formatTime(configured.getClosingTime()),
                true
        );
    }

    private OperatingHourExceptionResponse toExceptionResponse(EmployeeOperatingHourException exception) {
        return new OperatingHourExceptionResponse(
                exception.getId(),
                exception.getExceptionDate(),
                exception.isClosed(),
                exception.getOpeningTime() != null ? formatTime(exception.getOpeningTime()) : null,
                exception.getClosingTime() != null ? formatTime(exception.getClosingTime()) : null,
                exception.getReason(),
                exception.getUpdatedBy() != null ? exception.getUpdatedBy().getEmail() : null,
                exception.getUpdatedAt()
        );
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            return "Horario especial";
        }
        return reason.trim();
    }

    private String dayLabel(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "Lunes";
            case TUESDAY -> "Martes";
            case WEDNESDAY -> "Miércoles";
            case THURSDAY -> "Jueves";
            case FRIDAY -> "Viernes";
            case SATURDAY -> "Sábado";
            case SUNDAY -> "Domingo";
        };
    }

    private String formatTime(LocalTime time) {
        return time.format(TIME_FORMAT);
    }
}
