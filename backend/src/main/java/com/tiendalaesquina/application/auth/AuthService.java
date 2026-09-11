package com.tiendalaesquina.application.auth;

import com.tiendalaesquina.application.common.EmailNormalizer;
import com.tiendalaesquina.domain.model.OtpPurpose;
import com.tiendalaesquina.domain.model.Role;
import com.tiendalaesquina.domain.model.RoleName;
import com.tiendalaesquina.domain.model.UserAccount;
import com.tiendalaesquina.domain.repository.RoleRepository;
import com.tiendalaesquina.domain.repository.UserAccountRepository;
import com.tiendalaesquina.exception.ApiException;
import com.tiendalaesquina.exception.OtpRateLimitException;
import com.tiendalaesquina.security.JwtService;
import com.tiendalaesquina.web.dto.ChangePasswordRequest;
import com.tiendalaesquina.web.dto.ChallengeResponse;
import com.tiendalaesquina.web.dto.LoginResponse;
import com.tiendalaesquina.web.dto.MessageResponse;
import com.tiendalaesquina.web.dto.PasswordChangeResponse;
import com.tiendalaesquina.web.dto.RecoveryRequest;
import com.tiendalaesquina.web.dto.RegisterRequest;
import com.tiendalaesquina.web.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private static final String REGISTRATION_SENT = "Se envió un código de verificación al correo indicado";
    private static final String RECOVERY_SENT = "Si el correo está registrado, recibirá un código de recuperación";

    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final JwtService jwtService;

    public AuthService(UserAccountRepository users, RoleRepository roles, PasswordEncoder passwordEncoder,
                       OtpService otpService, JwtService jwtService) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.jwtService = jwtService;
    }

    @Transactional
    public ChallengeResponse register(RegisterRequest request) {
        ensurePasswordPolicy(request.password());
        String email = EmailNormalizer.normalize(request.email());
        if (users.findByEmail(email).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "email_already_registered", "Correo ya registrado",
                "El correo electrónico ya está registrado");
        }
        Role member = roles.findByName(RoleName.EMPLOYEE)
            .orElseThrow(() -> new IllegalStateException("EMPLOYEE role is missing from database migration"));
        UserAccount user = users.save(new UserAccount(email, passwordEncoder.encode(request.password()), member, false));
        var challenge = otpService.issue(user, OtpPurpose.REGISTRATION);
        return new ChallengeResponse(challenge.getId(), challenge.getExpiresAt(), REGISTRATION_SENT);
    }

    @Transactional
    public ChallengeResponse resendRegistration(String rawEmail) {
        UserAccount user = requireUser(rawEmail);
        if (user.isVerified()) {
            throw new ApiException(HttpStatus.CONFLICT, "account_already_verified", "Cuenta ya verificada",
                "La cuenta ya fue verificada");
        }
        var challenge = otpService.issue(user, OtpPurpose.REGISTRATION);
        return new ChallengeResponse(challenge.getId(), challenge.getExpiresAt(), REGISTRATION_SENT);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public MessageResponse verifyRegistration(String rawEmail, UUID challengeId, String code) {
        UserAccount user = requireUser(rawEmail);
        if (user.isVerified()) {
            throw new ApiException(HttpStatus.CONFLICT, "account_already_verified", "Cuenta ya verificada",
                "La cuenta ya fue verificada");
        }
        otpService.verify(challengeId, user, OtpPurpose.REGISTRATION, code);
        user.verify();
        users.save(user);
        return new MessageResponse("La cuenta fue verificada correctamente");
    }

    @Transactional
    public LoginResponse login(String rawEmail, String password) {
        UserAccount user = users.findByEmail(EmailNormalizer.normalize(rawEmail)).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "account_disabled", "Cuenta deshabilitada",
                "La cuenta está deshabilitada");
        }
        if (!user.isVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "account_not_verified", "Cuenta no verificada",
                "La cuenta no ha sido verificada");
        }
        if (user.isTwoFactorEnabled()) {
            var challenge = otpService.issue(user, OtpPurpose.LOGIN_2FA);
            return LoginResponse.challenge(challenge.getId());
        }
        return LoginResponse.token(jwtService.issue(user), jwtService.expirationTime());
    }

    @Transactional(noRollbackFor = ApiException.class)
    public LoginResponse verifyLogin(UUID challengeId, String code) {
        UserAccount user = otpService.verify(challengeId, OtpPurpose.LOGIN_2FA, code);
        ensureActive(user);
        return LoginResponse.token(jwtService.issue(user), jwtService.expirationTime());
    }

    @Transactional
    public MessageResponse requestRecovery(RecoveryRequest request) {
        String email = EmailNormalizer.normalize(request.email());
        UserAccount user = users.findByEmail(email).orElse(null);
        if (user != null && user.isEnabled() && user.isVerified()) {
            try {
                otpService.issue(user, OtpPurpose.PASSWORD_RECOVERY);
            } catch (OtpRateLimitException ignored) {
                // Keep recovery responses identical for known and unknown addresses.
            }
        }
        return new MessageResponse(RECOVERY_SENT);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public MessageResponse verifyRecovery(String rawEmail, String code, String newPassword) {
        ensurePasswordPolicy(newPassword);
        UserAccount user = users.findByEmail(EmailNormalizer.normalize(rawEmail)).orElse(null);
        if (user == null || !user.isEnabled() || !user.isVerified()) {
            throw invalidRecoveryOtp();
        }
        otpService.verifyLatest(user, OtpPurpose.PASSWORD_RECOVERY, code);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.incrementTokenVersion();
        users.save(user);
        return new MessageResponse("La contraseña fue restablecida correctamente");
    }

    @Transactional
    public PasswordChangeResponse changePassword(UserAccount user, ChangePasswordRequest request) {
        ensurePasswordPolicy(request.newPassword());
        ensureActive(user);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw invalidCredentials("La contraseña actual no es correcta");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_password_transition", "Contraseña inválida",
                "La nueva contraseña debe ser diferente de la actual");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.incrementTokenVersion();
        users.save(user);
        return new PasswordChangeResponse(jwtService.issue(user), "Bearer", jwtService.expirationTime(),
            "La contraseña fue cambiada correctamente");
    }

    @Transactional
    public ChallengeResponse requestTwoFactorChange(UserAccount user, String currentPassword,
                                                    OtpPurpose purpose) {
        ensureActive(user);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw invalidCredentials("La contraseña actual no es correcta");
        }
        boolean enabling = purpose == OtpPurpose.TWO_FACTOR_ENABLE;
        if (user.isTwoFactorEnabled() == enabling) {
            throw new ApiException(HttpStatus.CONFLICT, "two_factor_state_unchanged", "Estado sin cambios",
                "La autenticación de dos factores ya tiene ese estado");
        }
        var challenge = otpService.issue(user, purpose);
        return new ChallengeResponse(challenge.getId(), challenge.getExpiresAt(),
            "Se envió un código OTP para confirmar el cambio");
    }

    @Transactional(noRollbackFor = ApiException.class)
    public MessageResponse confirmTwoFactorChange(UserAccount user, UUID challengeId, String code,
                                                  OtpPurpose purpose) {
        ensureActive(user);
        otpService.verify(challengeId, user, purpose, code);
        user.setTwoFactorEnabled(purpose == OtpPurpose.TWO_FACTOR_ENABLE);
        users.save(user);
        return new MessageResponse(purpose == OtpPurpose.TWO_FACTOR_ENABLE
            ? "La autenticación de dos factores fue activada"
            : "La autenticación de dos factores fue desactivada");
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String email) {
        UserAccount user = requireUser(email);
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserAccount requireUser(String rawEmail) {
        return users.findByEmail(EmailNormalizer.normalize(rawEmail))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "account_not_found", "Cuenta no encontrada",
                "La cuenta no existe"));
    }

    public UserResponse toResponse(UserAccount user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole().getName().name(),
            user.isVerified(), user.isTwoFactorEnabled());
    }

    private void ensureActive(UserAccount user) {
        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "account_disabled", "Cuenta deshabilitada",
                "La cuenta está deshabilitada");
        }
        if (!user.isVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "account_not_verified", "Cuenta no verificada",
                "La cuenta no ha sido verificada");
        }
    }

    private ApiException invalidCredentials() {
        return invalidCredentials("Credenciales inválidas");
    }

    private ApiException invalidCredentials(String detail) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "invalid_credentials", "Credenciales inválidas", detail);
    }

    private void ensurePasswordPolicy(String password) {
        if (!PasswordPolicy.isValid(password)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_password", "Contraseña inválida",
                "La contraseña debe tener entre 8 y 72 caracteres, con mayúsculas, minúsculas y números");
        }
    }

    private ApiException invalidRecoveryOtp() {
        return new ApiException(HttpStatus.BAD_REQUEST, "invalid_otp", "Código OTP inválido",
            "Código OTP inválido o expirado");
    }
}
