-- V8: Seed permissions with fixed UUIDs
-- These permissions are used for role-based access control

INSERT INTO permissions (id, name, description, created_at, updated_at)
VALUES 
    ('b0000000-0000-0000-0000-000000000001', 'USER_READ', 'Permission to read user information', NOW(), NOW()),
    ('b0000000-0000-0000-0000-000000000002', 'USER_WRITE', 'Permission to create/update user information', NOW(), NOW()),
    ('b0000000-0000-0000-0000-000000000003', 'MOVIE_READ', 'Permission to read movie information', NOW(), NOW()),
    ('b0000000-0000-0000-0000-000000000004', 'MOVIE_WRITE', 'Permission to create/update/delete movies', NOW(), NOW()),
    ('b0000000-0000-0000-0000-000000000005', 'PAYMENT_READ', 'Permission to read payment information', NOW(), NOW()),
    ('b0000000-0000-0000-0000-000000000006', 'PAYMENT_WRITE', 'Permission to process payments', NOW(), NOW()),
    ('b0000000-0000-0000-0000-000000000007', 'ADMIN_ACCESS', 'Full administrative access', NOW(), NOW()),
    ('b0000000-0000-0000-0000-000000000008', 'CUSTOMER_READ', 'Permission to read customer profiles', NOW(), NOW()),
    ('b0000000-0000-0000-0000-000000000009', 'CUSTOMER_WRITE', 'Permission to update customer profiles', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Assign permissions to ADMIN role (all permissions)
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, NOW(), NOW()
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Assign basic permissions to USER role
INSERT INTO role_permissions (role_id, permission_id, created_at, updated_at)
SELECT r.id, p.id, NOW(), NOW()
FROM roles r, permissions p
WHERE r.name = 'USER' AND p.name IN ('USER_READ', 'MOVIE_READ', 'CUSTOMER_READ', 'CUSTOMER_WRITE')
ON CONFLICT (role_id, permission_id) DO NOTHING;
