-- Create a mock account (bypass sign-up)
INSERT INTO users (id,
                   email,
                   password,
                   first_name,
                   last_name,
                   role_id,
                   enabled,
                   account_verified_at,
                   created_at,
                   updated_at)
SELECT gen_random_uuid(),
       'mockuser@example.com',
       -- bcrypt hash for "12345678"
       '$2a$12$ACjze6ZkbWJothSIUZXjkOQCTzTjCtIfMZC3lobLsXdnfxmGtmOpq',
       'Mock',
       'User',
       r.id,
       TRUE,
       NOW(),
       NOW(),
       NOW()
FROM roles r
WHERE r.name = 'USER'
  AND NOT EXISTS (SELECT 1
                  FROM users u
                  WHERE u.email = 'mockuser@example.com');
