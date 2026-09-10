package com.tiendalaesquina.exception;

import org.springframework.http.HttpStatus;

public class OtpRateLimitException extends ApiException {

    public OtpRateLimitException() {
        super(HttpStatus.TOO_MANY_REQUESTS, "otp_rate_limited", "Demasiadas solicitudes",
            "Debe esperar antes de solicitar otro código OTP");
    }
}
