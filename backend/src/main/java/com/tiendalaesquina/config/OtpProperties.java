package com.tiendalaesquina.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "security.otp")
public class OtpProperties {

    private int length = 6;
    private Duration expiration = Duration.ofMinutes(10);
    private int maxAttempts = 5;
    private Duration resendCooldown = Duration.ofMinutes(1);

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public Duration getExpiration() {
        return expiration;
    }

    public void setExpiration(Duration expiration) {
        this.expiration = expiration;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getResendCooldown() {
        return resendCooldown;
    }

    public void setResendCooldown(Duration resendCooldown) {
        this.resendCooldown = resendCooldown;
    }
}
