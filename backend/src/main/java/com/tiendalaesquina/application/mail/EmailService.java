package com.tiendalaesquina.application.mail;

import com.tiendalaesquina.domain.model.OtpPurpose;

import java.time.Instant;

public interface EmailService {

    void sendOtp(String recipient, OtpPurpose purpose, String code, Instant expiresAt);
}
