-- V9: Update role UUIDs to fixed values for seed data consistency
-- This ensures cross-service data consistency with fixed UUIDs

-- Step 1: Temporarily disable foreign key checks by deferring constraints
-- Note: PostgreSQL handles FK updates automatically if we update in the right order

-- Step 2: Update ADMIN role to fixed UUID
UPDATE roles
SET id = 'a0000000-0000-0000-0000-000000000001'::uuid,
    updated_at = NOW()
WHERE name = 'ADMIN'
  AND id != 'a0000000-0000-0000-0000-000000000001'::uuid;

-- Step 3: Update USER role to fixed UUID  
UPDATE roles
SET id = 'a0000000-0000-0000-0000-000000000002'::uuid,
    updated_at = NOW()
WHERE name = 'USER'
  AND id != 'a0000000-0000-0000-0000-000000000002'::uuid;

-- Note: The users table has FK constraint to roles(id)
-- PostgreSQL will not cascade UUID changes automatically
-- We need to update users.role_id references first if there are existing users

-- Update existing users' role_id to new ADMIN UUID
UPDATE users u
SET role_id = 'a0000000-0000-0000-0000-000000000001'::uuid
FROM roles r
WHERE u.role_id = r.id
  AND r.name = 'ADMIN'
  AND u.role_id != 'a0000000-0000-0000-0000-000000000001'::uuid;

-- Update existing users' role_id to new USER UUID
UPDATE users u
SET role_id = 'a0000000-0000-0000-0000-000000000002'::uuid
FROM roles r
WHERE u.role_id = r.id
  AND r.name = 'USER'
  AND u.role_id != 'a0000000-0000-0000-0000-000000000002'::uuid;
