-- V3__add_refresh_tokens.sql
-- Adds refresh token support for JWT authentication

CREATE TABLE refresh_token (
    id BIGSERIAL PRIMARY KEY,

    token VARCHAR(500) NOT NULL UNIQUE,

    expiry_date TIMESTAMP NOT NULL,

    user_id BIGINT NOT NULL,

    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- Index for faster lookups
CREATE INDEX idx_refresh_token_token
ON refresh_token(token);

CREATE INDEX idx_refresh_token_user
ON refresh_token(user_id);