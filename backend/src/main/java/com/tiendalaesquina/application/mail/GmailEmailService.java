package com.tiendalaesquina.application.mail;

import com.tiendalaesquina.domain.model.OtpPurpose;
import com.tiendalaesquina.exception.ApiException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class GmailEmailService implements EmailService {

    private static final DateTimeFormatter EXPIRATION_FORMAT = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm 'UTC'")
        .withZone(ZoneOffset.UTC);

    private final JavaMailSender mailSender;
    private final String sender;
    private final String senderPassword;

    public GmailEmailService(JavaMailSender mailSender,
                             @Value("${spring.mail.username}") String sender,
                             @Value("${spring.mail.password}") String senderPassword) {
        this.mailSender = mailSender;
        this.sender = sender;
        this.senderPassword = senderPassword;
    }

    @PostConstruct
    void validateConfiguration() {
        if (sender == null || sender.isBlank() || senderPassword == null || senderPassword.isBlank()) {
            throw new IllegalStateException("EMAIL_SENDER_APP and EMAIL_SENDER_PASSWORD are required");
        }
    }

    @Override
    public void sendOtp(String recipient, OtpPurpose purpose, String code, Instant expiresAt) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(recipient);
        message.setSubject(subjectFor(purpose));
        message.setText("Su código de verificación es: " + code + "\n"
            + "Este código corresponde a: " + purposeLabel(purpose) + ".\n"
            + "Expira el " + EXPIRATION_FORMAT.format(expiresAt) + ".\n"
            + "Si no solicitó esta operación, ignore este mensaje.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "email_delivery_failed",
                "Servicio de correo no disponible", "No fue posible enviar el correo de verificación");
        }
    }

    private String subjectFor(OtpPurpose purpose) {
        return switch (purpose) {
            case REGISTRATION -> "Código de verificación de registro";
            case LOGIN_2FA -> "Código de autenticación de dos factores";
            case PASSWORD_RECOVERY -> "Código de recuperación de contraseña";
            case TWO_FACTOR_ENABLE -> "Confirmación de activación de dos factores";
            case TWO_FACTOR_DISABLE -> "Confirmación de desactivación de dos factores";
        };
    }

    private String purposeLabel(OtpPurpose purpose) {
        return switch (purpose) {
            case REGISTRATION -> "registro de cuenta";
            case LOGIN_2FA -> "inicio de sesión";
            case PASSWORD_RECOVERY -> "recuperación de contraseña";
            case TWO_FACTOR_ENABLE -> "activación de dos factores";
            case TWO_FACTOR_DISABLE -> "desactivación de dos factores";
        };
    }
}
