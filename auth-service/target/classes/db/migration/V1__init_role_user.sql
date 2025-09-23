-- extension pgcrypto; -- Sử dụng pgcrypto để tạo UUID
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Tạo bảng role
CREATE TABLE roles
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    deleted_at  TIMESTAMP
)
;

-- Thêm role mặc định
INSERT INTO roles (name, description)
VALUES ('ADMIN', 'Administrator role'),
       ('USER', 'Regular user role');

-- Tạo bảng users với created_at và deleted_at
CREATE TABLE users
(
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email               VARCHAR(255) NOT NULL UNIQUE,
    password            VARCHAR(255) NOT NULL,
    first_name          VARCHAR(50),
    last_name           VARCHAR(50),
    role_id             UUID          NOT NULL,
    enabled             BOOLEAN   DEFAULT FALSE,
    account_verified_at TIMESTAMP,
    created_at          TIMESTAMP DEFAULT NOW(),
    deleted_at          TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT NOW(),
    CONSTRAINT fk_role FOREIGN KEY (role_id) REFERENCES roles (id)
);
