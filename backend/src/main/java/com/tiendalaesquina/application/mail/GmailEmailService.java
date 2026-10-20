package com.tiendalaesquina.application.mail;

import com.tiendalaesquina.domain.model.OtpPurpose;
import com.tiendalaesquina.domain.model.StockNotificationType;
import com.tiendalaesquina.exception.ApiException;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class GmailEmailService implements EmailService {

    private static final DateTimeFormatter EXPIRATION_FORMAT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm 'UTC'")
            .withZone(ZoneOffset.UTC);

    private static final DateTimeFormatter GUATEMALA_FORMAT = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("America/Guatemala"));

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

    @Override
    public void sendStockAlert(String recipient,
                               StockNotificationType notificationType,
                               String productCode,
                               String productName,
                               BigDecimal currentStock,
                               BigDecimal minimumStock) {
        String title = notificationType == StockNotificationType.OUT_OF_STOCK
                ? "Producto agotado"
                : "Existencia baja";

        String accent = notificationType == StockNotificationType.OUT_OF_STOCK
                ? "#b42318"
                : "#9a6700";

        String recommendation = notificationType == StockNotificationType.OUT_OF_STOCK
                ? "Se recomienda realizar una reposición lo antes posible para evitar perder ventas."
                : "Se recomienda revisar la existencia y programar una reposición antes de que el producto se agote.";

        String safeCode = escapeHtml(productCode);
        String safeName = escapeHtml(productName);
        String stock = formatQuantity(currentStock);
        String minimum = formatQuantity(minimumStock);

        String html = """
                <!doctype html>
                <html lang=\"es\">
                  <body style=\"margin:0;background:#f6f2e8;font-family:Arial,sans-serif;color:#173f34;\">
                    <div style=\"max-width:620px;margin:24px auto;background:#ffffff;border:1px solid #e4ded0;border-radius:16px;overflow:hidden;\">
                      <div style=\"background:#123f34;padding:22px 28px;color:#ffffff;\">
                        <div style=\"font-size:22px;font-weight:700;\">Tienda La Esquina</div>
                        <div style=\"font-size:13px;margin-top:4px;opacity:.85;\">Alerta automática de inventario</div>
                      </div>
                      <div style=\"padding:26px 28px;\">
                        <div style=\"display:inline-block;padding:7px 12px;border-radius:999px;background:#fff3c6;color:%s;font-size:12px;font-weight:700;\">%s</div>
                        <h2 style=\"margin:18px 0 8px;color:#173f34;\">%s</h2>
                        <p style=\"margin:0 0 20px;color:#667069;line-height:1.55;\">El inventario del producto cambió a un estado que requiere atención.</p>
                        <table style=\"width:100%%;border-collapse:collapse;font-size:14px;\">
                          <tr><td style=\"padding:9px 0;color:#777;\">Código</td><td style=\"padding:9px 0;text-align:right;font-weight:700;\">%s</td></tr>
                          <tr><td style=\"padding:9px 0;color:#777;\">Existencia actual</td><td style=\"padding:9px 0;text-align:right;font-weight:700;color:%s;\">%s</td></tr>
                          <tr><td style=\"padding:9px 0;color:#777;\">Existencia mínima</td><td style=\"padding:9px 0;text-align:right;font-weight:700;\">%s</td></tr>
                          <tr><td style=\"padding:9px 0;color:#777;\">Fecha de alerta</td><td style=\"padding:9px 0;text-align:right;font-weight:700;\">%s</td></tr>
                        </table>
                        <div style=\"margin-top:22px;padding:14px 16px;border-left:4px solid #f4c542;background:#fff9e7;color:#5f5a4a;line-height:1.5;font-size:13px;\">%s</div>
                      </div>
                      <div style=\"padding:16px 28px;background:#f5f7f5;color:#7a817c;font-size:11px;\">Este correo fue generado automáticamente por el Sistema Tienda La Esquina.</div>
                    </div>
                  </body>
                </html>
                """.formatted(
                accent,
                title.toUpperCase(),
                safeName,
                safeCode,
                accent,
                stock,
                minimum,
                GUATEMALA_FORMAT.format(Instant.now()),
                recommendation
        );

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    false,
                    StandardCharsets.UTF_8.name()
            );
            helper.setFrom(sender);
            helper.setTo(recipient);
            helper.setSubject("[Tienda La Esquina] " + title + ": " + productCode);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException | MailException exception) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "stock_alert_email_failed",
                    "No fue posible enviar la alerta por correo",
                    "La alerta de inventario fue registrada, pero el correo no pudo ser enviado"
            );
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

    private String formatQuantity(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
