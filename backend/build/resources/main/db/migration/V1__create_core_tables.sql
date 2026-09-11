CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(32) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    token_version INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    role_id BIGINT NOT NULL REFERENCES roles (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT app_users_email_lowercase CHECK (email = lower(email)),
    CONSTRAINT app_users_token_version_nonnegative CHECK (token_version >= 0)
);

CREATE TABLE otp_challenges (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    purpose VARCHAR(32) NOT NULL,
    code_hash VARCHAR(100) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    consumed_at TIMESTAMPTZ,
    CONSTRAINT otp_challenges_attempts_nonnegative CHECK (attempts >= 0),
    CONSTRAINT otp_challenges_max_attempts_positive CHECK (max_attempts > 0),
    CONSTRAINT otp_challenges_purpose_valid CHECK (
        purpose IN ('REGISTRATION', 'LOGIN_2FA', 'PASSWORD_RECOVERY', 'TWO_FACTOR_ENABLE', 'TWO_FACTOR_DISABLE')
    )
);

CREATE INDEX idx_otp_challenges_user_purpose_created
    ON otp_challenges (user_id, purpose, created_at DESC);

CREATE INDEX idx_otp_challenges_expiration
    ON otp_challenges (expires_at);
