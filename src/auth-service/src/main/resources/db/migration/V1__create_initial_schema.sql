-- V1: Create initial schema for auth service
-- Includes all tables with full schema, audit fields, and soft delete support

-- Enable pgcrypto extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create function to generate UUIDv7
CREATE OR REPLACE FUNCTION uuid_generate_v7() RETURNS uuid AS $$
BEGIN
    RETURN (
        lpad(to_hex((extract(epoch from clock_timestamp()) * 1000)::bigint), 12, '0') ||
        '7' ||
        substring(encode(gen_random_bytes(10), 'hex') from 1 for 19)
    )::uuid;
END;
$$ LANGUAGE plpgsql;

-- Create roles table
CREATE TABLE roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    deleted_at  TIMESTAMP
);

-- Create users table
CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email               VARCHAR(255) NOT NULL UNIQUE,
    password            VARCHAR(255),
    first_name          VARCHAR(50),
    last_name           VARCHAR(50),
    role_id             UUID NOT NULL,
    enabled             BOOLEAN DEFAULT FALSE,
    account_verified_at TIMESTAMP,
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW(),
    deleted_at          TIMESTAMP,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

-- Create tokens table
CREATE TABLE tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content     VARCHAR(255) NOT NULL,
    type        VARCHAR(50) NOT NULL,
    expire_time TIMESTAMP NOT NULL,
    user_id     UUID NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    deleted_at  TIMESTAMP,
    CONSTRAINT fk_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Create permissions table
CREATE TABLE permissions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW(),
    deleted_at  TIMESTAMP
);

-- Create role_permissions table for many-to-many relationship
CREATE TABLE role_permissions (
    role_id       UUID NOT NULL,
    permission_id UUID NOT NULL,
    created_at    TIMESTAMP DEFAULT NOW(),
    updated_at    TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
);

-- Create user_devices table
CREATE TABLE user_devices (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL,
    device_name    VARCHAR(255) NOT NULL,
    device_type    VARCHAR(50),
    device_os      VARCHAR(100),
    browser        VARCHAR(100),
    ip_address     VARCHAR(45),
    user_agent     TEXT,
    is_current     BOOLEAN DEFAULT FALSE,
    last_active_at TIMESTAMP DEFAULT NOW(),
    created_at     TIMESTAMP DEFAULT NOW(),
    updated_at     TIMESTAMP DEFAULT NOW(),
    deleted_at     TIMESTAMP,
    CONSTRAINT fk_user_devices_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_role_id ON users (role_id);
CREATE INDEX idx_tokens_user_id ON tokens (user_id);
CREATE INDEX idx_tokens_expire_time ON tokens (expire_time);
CREATE INDEX idx_permissions_name ON permissions (name);
CREATE INDEX idx_role_permissions_role_id ON role_permissions (role_id);
CREATE INDEX idx_role_permissions_permission_id ON role_permissions (permission_id);
CREATE INDEX idx_user_devices_user_id ON user_devices (user_id);
CREATE INDEX idx_user_devices_last_active ON user_devices (last_active_at);
CREATE INDEX idx_user_devices_is_current ON user_devices (is_current);