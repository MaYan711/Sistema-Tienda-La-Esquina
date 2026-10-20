package com.tiendalaesquina.application.user;

import com.tiendalaesquina.application.auth.PasswordPolicy;
import com.tiendalaesquina.application.common.EmailNormalizer;
import com.tiendalaesquina.domain.model.AuditEvent;
import com.tiendalaesquina.domain.model.Role;
import com.tiendalaesquina.domain.model.RoleName;
import com.tiendalaesquina.domain.model.UserAccount;
import com.tiendalaesquina.domain.repository.AuditEventRepository;
import com.tiendalaesquina.domain.repository.RoleRepository;
import com.tiendalaesquina.domain.repository.UserAccountRepository;
import com.tiendalaesquina.exception.ApiException;
import com.tiendalaesquina.web.dto.CreateUserRequest;
import com.tiendalaesquina.web.dto.UpdateUserRequest;
import com.tiendalaesquina.web.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

@Service
public class UserService {

    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventRepository auditEvents;

    public UserService(UserAccountRepository users,
                       RoleRepository roles,
                       PasswordEncoder passwordEncoder,
                       AuditEventRepository auditEvents) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.auditEvents = auditEvents;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> search(String search, String role, Boolean enabled, Pageable pageable) {
        String normalizedSearch = normalizeNullable(search);
        RoleName roleNameFilter = parseRoleFilter(role);

        Specification<UserAccount> specification = (root, query, cb) -> cb.conjunction();

        if (normalizedSearch != null) {
            String pattern = "%" + normalizedSearch.toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) ->
                cb.like(cb.lower(root.get("email")), pattern)
            );
        }

        if (roleNameFilter != null) {
            specification = specification.and((root, query, cb) ->
                cb.equal(root.get("role").get("name"), roleNameFilter)
            );
        }

        if (enabled != null) {
            specification = specification.and((root, query, cb) ->
                cb.equal(root.get("enabled"), enabled)
            );
        }

        return users.findAll(specification, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return toResponse(requireUser(id));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request, String actorEmail) {
        String email = EmailNormalizer.normalize(request.email());
        if (users.findByEmail(email).isPresent()) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                "email_already_registered",
                "Correo ya registrado",
                "El correo electrónico ya está registrado"
            );
        }

        if (!PasswordPolicy.isValid(request.password())) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "invalid_password",
                "Contraseña inválida",
                "La contraseña debe tener entre 8 y 72 caracteres, con mayúsculas, minúsculas y números"
            );
        }

        Role role = parseAndRequireRole(request.role());
        UserAccount actor = requireActor(actorEmail);

        UserAccount newUser = new UserAccount(
            email,
            passwordEncoder.encode(request.password()),
            role,
            true
        );
        newUser.enable();
        newUser = users.save(newUser);

        String summary = String.format("Usuario creado: %s con rol %s", newUser.getEmail(), role.getName().name());
        Map<String, Object> afterData = Map.of(
            "email", newUser.getEmail(),
            "role", role.getName().name(),
            "verified", newUser.isVerified(),
            "enabled", newUser.isEnabled()
        );

        auditEvents.save(new AuditEvent(
            actor,
            "USER_CREATED",
            "USER",
            String.valueOf(newUser.getId()),
            summary,
            null,
            afterData,
            null,
            null,
            Instant.now()
        ));

        return toResponse(newUser);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request, String actorEmail) {
        UserAccount target = requireUser(id);
        UserAccount actor = requireActor(actorEmail);

        String newEmail = EmailNormalizer.normalize(request.email());
        if (users.existsByEmailIgnoreCaseAndIdNot(newEmail, id)) {
            throw new ApiException(
                HttpStatus.CONFLICT,
                "email_already_registered",
                "Correo ya registrado",
                "El correo electrónico ya está registrado por otro usuario"
            );
        }

        Role newRole = parseAndRequireRole(request.role());

        boolean isSelf = actor.getId().equals(target.getId())
            || actor.getEmail().equalsIgnoreCase(target.getEmail());

        if (isSelf && target.getRole().getName() == RoleName.ADMIN && newRole.getName() != RoleName.ADMIN) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "self_role_change_not_allowed",
                "Operación no permitida",
                "El administrador no puede quitarse su propio rol de ADMIN"
            );
        }

        String oldEmail = target.getEmail();
        RoleName oldRoleName = target.getRole().getName();
        boolean emailChanged = !oldEmail.equalsIgnoreCase(newEmail);
        boolean roleChanged = oldRoleName != newRole.getName();

        if (emailChanged || roleChanged) {
            target.incrementTokenVersion();
            if (emailChanged) {
                target.setEmail(newEmail);
            }
            if (roleChanged) {
                target.setRole(newRole);
            }
            target = users.save(target);

            String action = roleChanged && !emailChanged ? "ROLE_CHANGED" : "USER_UPDATED";
            String summary = String.format(
                "Usuario actualizado: ID %d (%s -> %s, rol %s -> %s)",
                id, oldEmail, newEmail, oldRoleName, newRole.getName()
            );
            if (summary.length() > 255) {
                summary = summary.substring(0, 252) + "...";
            }

            Map<String, Object> beforeData = Map.of(
                "email", oldEmail,
                "role", oldRoleName.name()
            );
            Map<String, Object> afterData = Map.of(
                "email", newEmail,
                "role", newRole.getName().name()
            );

            auditEvents.save(new AuditEvent(
                actor,
                action,
                "USER",
                String.valueOf(target.getId()),
                summary,
                beforeData,
                afterData,
                null,
                null,
                Instant.now()
            ));
        }

        return toResponse(target);
    }

    @Transactional
    public UserResponse setStatus(Long id, boolean enabled, String actorEmail) {
        UserAccount target = requireUser(id);
        UserAccount actor = requireActor(actorEmail);

        boolean isSelf = actor.getId().equals(target.getId())
            || actor.getEmail().equalsIgnoreCase(target.getEmail());

        if (!enabled && isSelf) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "self_deactivation_not_allowed",
                "Operación no permitida",
                "El administrador no puede desactivarse a sí mismo"
            );
        }

        if (target.isEnabled() != enabled) {
            boolean wasEnabled = target.isEnabled();
            if (enabled) {
                target.enable();
            } else {
                target.disable();
            }
            target.incrementTokenVersion();
            target = users.save(target);

            String action = enabled ? "USER_ACTIVATED" : "USER_DEACTIVATED";
            String summary = String.format(
                "Usuario %s: %s (ID: %d)",
                enabled ? "activado" : "desactivado", target.getEmail(), target.getId()
            );
            if (summary.length() > 255) {
                summary = summary.substring(0, 252) + "...";
            }

            Map<String, Object> beforeData = Map.of("enabled", wasEnabled);
            Map<String, Object> afterData = Map.of("enabled", enabled);

            auditEvents.save(new AuditEvent(
                actor,
                action,
                "USER",
                String.valueOf(target.getId()),
                summary,
                beforeData,
                afterData,
                null,
                null,
                Instant.now()
            ));
        }

        return toResponse(target);
    }

    public UserResponse toResponse(UserAccount user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getRole().getName().name(),
            user.isVerified(),
            user.isEnabled(),
            user.isTwoFactorEnabled(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    private UserAccount requireUser(Long id) {
        return users.findById(id)
            .orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "user_not_found",
                "Usuario no encontrado",
                "El usuario solicitado no existe"
            ));
    }

    private UserAccount requireActor(String email) {
        return users.findByEmail(EmailNormalizer.normalize(email))
            .orElseThrow(() -> new ApiException(
                HttpStatus.UNAUTHORIZED,
                "account_not_found",
                "Cuenta no encontrada",
                "No fue posible identificar al usuario autenticado"
            ));
    }

    private Role parseAndRequireRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "invalid_role",
                "Rol inválido",
                "El rol es obligatorio"
            );
        }

        RoleName roleName;
        try {
            roleName = RoleName.valueOf(rawRole.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "invalid_role",
                "Rol inválido",
                "El rol debe ser ADMIN o EMPLOYEE"
            );
        }

        return roles.findByName(roleName)
            .orElseThrow(() -> new ApiException(
                HttpStatus.BAD_REQUEST,
                "invalid_role",
                "Rol inválido",
                "El rol especificado no existe"
            ));
    }

    private RoleName parseRoleFilter(String rawRole) {
        String normalized = normalizeNullable(rawRole);
        if (normalized == null) {
            return null;
        }

        try {
            return RoleName.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "invalid_role",
                "Rol inválido",
                "El rol para filtrar debe ser ADMIN o EMPLOYEE"
            );
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
