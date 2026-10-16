package com.tiendalaesquina.application.mail;

import com.tiendalaesquina.domain.model.OtpPurpose;
import com.tiendalaesquina.domain.model.StockNotificationType;

import java.math.BigDecimal;
import java.time.Instant;

public interface EmailService {

    void sendOtp(String recipient, OtpPurpose purpose, String code, Instant expiresAt);

    default void sendStockAlert(String recipient,
                                StockNotificationType notificationType,
                                String productCode,
                                String productName,
                                BigDecimal currentStock,
                                BigDecimal minimumStock) {

    }
}
