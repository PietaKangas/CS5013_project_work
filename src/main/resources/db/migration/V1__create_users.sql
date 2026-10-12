-- V1: user accounts (login data only; student profile data comes in a later migration)
CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role  CHECK (role IN ('STUDENT', 'LEAD'))
);
