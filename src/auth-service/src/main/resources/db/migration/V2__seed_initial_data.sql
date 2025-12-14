-- V2: Seed initial data for auth service
-- Insert default roles, permissions with random UUIDv7, assign permissions to roles, and create a mock user

-- Insert default roles with random UUIDv7
INSERT INTO roles (id, name, description, created_at, updated_at) VALUES
(uuid_generate_v7(), 'ADMIN', 'Administrator role', NOW(), NOW()),
(uuid_generate_v7(), 'USER', 'Regular user role', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- Insert permissions with random UUIDv7
INSERT INTO permissions (id, name, description, created_at, updated_at) VALUES
(uuid_generate_v7(), 'USER_READ', 'Permission to read user information', NOW(), NOW()),
(uuid_generate_v7(), 'USER_WRITE', 'Permission to create/update user information', NOW(), NOW()),
(uuid_generate_v7(), 'MOVIE_READ', 'Permission to read movie information', NOW(), NOW()),
(uuid_generate_v7(), 'MOVIE_WRITE', 'Permission to create/update/delete movies', NOW(), NOW()),
(uuid_generate_v7(), 'PAYMENT_READ', 'Permission to read payment information', NOW(), NOW()),
(uuid_generate_v7(), 'PAYMENT_WRITE', 'Permission to process payments', NOW(), NOW()),
(uuid_generate_v7(), 'ADMIN_ACCESS', 'Full administrative access', NOW(), NOW()),
(uuid_generate_v7(), 'CUSTOMER_READ', 'Permission to read customer profiles', NOW(), NOW()),
(uuid_generate_v7(), 'CUSTOMER_WRITE', 'Permission to update customer profiles', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- Assign all permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, NOW(), NOW()
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Assign basic permissions to USER role
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, NOW(), NOW()
FROM roles r
JOIN permissions p ON p.name IN ('USER_READ', 'MOVIE_READ', 'CUSTOMER_READ', 'CUSTOMER_WRITE')
WHERE r.name = 'USER'
ON CONFLICT (role_id, permission_id) DO NOTHING;