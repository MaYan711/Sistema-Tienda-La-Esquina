package com.tiendalaesquina.application.auth;

import com.tiendalaesquina.application.mail.EmailService;
import com.tiendalaesquina.config.OtpProperties;
import com.tiendalaesquina.domain.model.OtpChallenge;
import com.tiendalaesquina.domain.model.OtpPurpose;
import com.tiendalaesquina.domain.model.UserAccount;
import com.tiendalaesquina.domain.repository.OtpChallengeRepository;
import com.tiendalaesquina.exception.ApiException;
import com.tiendalaesquina.exception.OtpRateLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class OtpService {

    private final OtpChallengeRepository challenges;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final OtpProperties properties;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpChallengeRepository challenges, PasswordEncoder passwordEncoder,
                      EmailService emailService, OtpProperties properties) {
        this.challenges = challenges;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.properties = properties;
        validateConfiguration();
    }

    @Transactional(noRollbackFor = OtpRateLimitException.class)
    public OtpChallenge issue(UserAccount user, OtpPurpose purpose) {
        Instant now = Instant.now();
        OtpChallenge latest = challenges.findTopByUserAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(user, purpose)
            .orElse(null);
        if (latest != null && !latest.isExpired(now)
            && latest.getCreatedAt().plus(properties.getResendCooldown()).isAfter(now)) {
            throw new OtpRateLimitException();
        }

        challenges.invalidateActive(user, purpose, now);
        String code = generateCode();
        Instant expiresAt = now.plus(properties.getExpiration());
        OtpChallenge challenge = new OtpChallenge(UUID.randomUUID(), user, purpose,
            passwordEncoder.encode(code), expiresAt, properties.getMaxAttempts(), now);
        OtpChallenge saved = challenges.save(challenge);
        emailService.sendOtp(user.getEmail(), purpose, code, expiresAt);
        return saved;
    }

    @Transactional(noRollbackFor = ApiException.class)
    public UserAccount verify(UUID challengeId, UserAccount expectedUser, OtpPurpose purpose, String code) {
        OtpChallenge challenge = challenges.findForUpdate(challengeId).orElse(null);
        if (challenge == null || challenge.getPurpose() != purpose
            || !challenge.getUser().getId().equals(expectedUser.getId())) {
            throw invalidOtp();
        }
        verifyCode(challenge, code);
        challenge.consume(Instant.now());
        challenges.save(challenge);
        return challenge.getUser();
    }

    @Transactional(noRollbackFor = ApiException.class)
    public UserAccount verify(UUID challengeId, OtpPurpose purpose, String code) {
        OtpChallenge challenge = challenges.findForUpdate(challengeId).orElse(null);
        if (challenge == null || challenge.getPurpose() != purpose) {
            throw invalidOtp();
        }
        verifyCode(challenge, code);
        challenge.consume(Instant.now());
        challenges.save(challenge);
        return challenge.getUser();
    }

    @Transactional(noRollbackFor = ApiException.class)
    public UserAccount verifyLatest(UserAccount expectedUser, OtpPurpose purpose, String code) {
        OtpChallenge challenge = challenges.findLatestActiveForUpdate(expectedUser, purpose).orElse(null);
        if (challenge == null) {
            throw invalidOtp();
        }
        verifyCode(challenge, code);
        challenge.consume(Instant.now());
        challenges.save(challenge);
        return challenge.getUser();
    }

    public Duration expiration() {
        return properties.getExpiration();
    }

    private void verifyCode(OtpChallenge challenge, String code) {
        Instant now = Instant.now();
        if (code == null || challenge.isConsumed() || challenge.isExpired(now)
            || !challenge.hasAttemptsRemaining()) {
            throw invalidOtp();
        }
        if (!passwordEncoder.matches(code, challenge.getCodeHash())) {
            challenge.registerFailedAttempt(now);
            challenges.save(challenge);
            throw invalidOtp();
        }
    }

    private String generateCode() {
        int length = properties.getLength();
        int bound = (int) Math.pow(10, length);
        return String.format(Locale.ROOT, "%0" + length + "d", random.nextInt(bound));
    }

    private void validateConfiguration() {
        if (properties.getLength() < 4 || properties.getLength() > 8
            || properties.getExpiration() == null || properties.getExpiration().isZero()
            || properties.getExpiration().isNegative() || properties.getMaxAttempts() < 1
            || properties.getResendCooldown() == null || properties.getResendCooldown().isNegative()) {
            throw new IllegalStateException("OTP configuration is invalid");
        }
    }

    private ApiException invalidOtp() {
        return new ApiException(HttpStatus.BAD_REQUEST, "invalid_otp", "Código OTP inválido",
            "Código OTP inválido o expirado");
    }
}
