-- V4: Tạo bảng permissions và quan hệ nhiều-nhiều với roles

-- Tạo bảng permissions
CREATE TABLE permissions
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMP        DEFAULT NOW(),
    updated_at  TIMESTAMP        DEFAULT NOW(),
    deleted_at  TIMESTAMP
);

-- Tạo bảng trung gian role_permissions cho quan hệ nhiều-nhiều
CREATE TABLE role_permissions
(
    role_id       UUID NOT NULL,
    permission_id UUID NOT NULL,
    created_at    TIMESTAMP DEFAULT NOW(),
    updated_at    TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_perm_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_perm_perm FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
);
