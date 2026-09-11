package com.tiendalaesquina.web.auth;

import com.tiendalaesquina.application.auth.AuthService;
import com.tiendalaesquina.domain.model.OtpPurpose;
import com.tiendalaesquina.web.dto.ChangePasswordRequest;
import com.tiendalaesquina.web.dto.ChallengeResponse;
import com.tiendalaesquina.web.dto.LoginRequest;
import com.tiendalaesquina.web.dto.LoginResponse;
import com.tiendalaesquina.web.dto.MessageResponse;
import com.tiendalaesquina.web.dto.PasswordChangeResponse;
import com.tiendalaesquina.web.dto.RecoveryRequest;
import com.tiendalaesquina.web.dto.RegisterRequest;
import com.tiendalaesquina.web.dto.TwoFactorRequest;
import com.tiendalaesquina.web.dto.UserResponse;
import com.tiendalaesquina.web.dto.VerifyChallengeRequest;
import com.tiendalaesquina.web.dto.VerifyLoginRequest;
import com.tiendalaesquina.web.dto.VerifyRecoveryRequest;
import com.tiendalaesquina.web.dto.VerifyRegistrationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación", description = "Registro, inicio de sesión, OTP y contraseñas")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar una cuenta", description = "Crea una cuenta pendiente y envía un OTP de registro")
    @ResponseStatus(HttpStatus.CREATED)
    public ChallengeResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping({"/register/resend", "/registration/resend"})
    @Operation(summary = "Reenviar el OTP de registro")
    public ChallengeResponse resendRegistration(@Valid @RequestBody RecoveryRequest request) {
        return authService.resendRegistration(request.email());
    }

    @PostMapping({"/register/verify", "/verify-registration"})
    @Operation(summary = "Verificar el registro")
    public MessageResponse verifyRegistration(@Valid @RequestBody VerifyRegistrationRequest request) {
        return authService.verifyRegistration(request.email(), request.challengeId(), request.otp());
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Emite un JWT o inicia un desafío OTP de dos factores")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    @PostMapping({"/login/verify", "/verify-login"})
    @Operation(summary = "Verificar el OTP de inicio de sesión")
    public LoginResponse verifyLogin(@Valid @RequestBody VerifyLoginRequest request) {
        return authService.verifyLogin(request.challengeId(), request.otp());
    }

    @PostMapping({"/password-recovery", "/forgot-password"})
    @Operation(summary = "Solicitar recuperación de contraseña")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MessageResponse requestRecovery(@Valid @RequestBody RecoveryRequest request) {
        return authService.requestRecovery(request);
    }

    @PostMapping({"/password-recovery/verify", "/reset-password"})
    @Operation(summary = "Restablecer la contraseña con OTP")
    public MessageResponse verifyRecovery(@Valid @RequestBody VerifyRecoveryRequest request) {
        return authService.verifyRecovery(request.email(), request.otp(), request.newPassword());
    }

    @PostMapping({"/password/change", "/change-password"})
    @Operation(summary = "Cambiar la contraseña", security = @SecurityRequirement(name = "bearerAuth"))
    @SecurityRequirement(name = "bearerAuth")
    public PasswordChangeResponse changePassword(Authentication authentication,
                                                 @Valid @RequestBody ChangePasswordRequest request) {
        return authService.changePassword(authService.requireUser(authentication.getName()), request);
    }

    @PostMapping("/2fa/enable")
    @Operation(summary = "Solicitar activación de dos factores", security = @SecurityRequirement(name = "bearerAuth"))
    @SecurityRequirement(name = "bearerAuth")
    public ChallengeResponse requestTwoFactorEnable(Authentication authentication,
                                                    @Valid @RequestBody TwoFactorRequest request) {
        return authService.requestTwoFactorChange(authService.requireUser(authentication.getName()),
            request.currentPassword(), OtpPurpose.TWO_FACTOR_ENABLE);
    }

    @PostMapping("/2fa/enable/verify")
    @Operation(summary = "Confirmar activación de dos factores", security = @SecurityRequirement(name = "bearerAuth"))
    @SecurityRequirement(name = "bearerAuth")
    public MessageResponse confirmTwoFactorEnable(Authentication authentication,
                                                  @Valid @RequestBody VerifyChallengeRequest request) {
        return authService.confirmTwoFactorChange(authService.requireUser(authentication.getName()),
            request.challengeId(), request.otp(), OtpPurpose.TWO_FACTOR_ENABLE);
    }

    @PostMapping("/2fa/disable")
    @Operation(summary = "Solicitar desactivación de dos factores", security = @SecurityRequirement(name = "bearerAuth"))
    @SecurityRequirement(name = "bearerAuth")
    public ChallengeResponse requestTwoFactorDisable(Authentication authentication,
                                                     @Valid @RequestBody TwoFactorRequest request) {
        return authService.requestTwoFactorChange(authService.requireUser(authentication.getName()),
            request.currentPassword(), OtpPurpose.TWO_FACTOR_DISABLE);
    }

    @PostMapping("/2fa/disable/verify")
    @Operation(summary = "Confirmar desactivación de dos factores", security = @SecurityRequirement(name = "bearerAuth"))
    @SecurityRequirement(name = "bearerAuth")
    public MessageResponse confirmTwoFactorDisable(Authentication authentication,
                                                   @Valid @RequestBody VerifyChallengeRequest request) {
        return authService.confirmTwoFactorChange(authService.requireUser(authentication.getName()),
            request.challengeId(), request.otp(), OtpPurpose.TWO_FACTOR_DISABLE);
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar la cuenta autenticada", security = @SecurityRequirement(name = "bearerAuth"))
    @SecurityRequirement(name = "bearerAuth")
    public UserResponse currentUser(Authentication authentication) {
        return authService.currentUser(authentication.getName());
    }
}
