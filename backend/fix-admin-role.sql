-- Step 1: Check current admin user roles
SELECT u.id, u.username, u.email, ur.role 
FROM users u 
LEFT JOIN user_roles ur ON u.id = ur.user_id 
WHERE u.username = 'admin';

-- Step 2: Fix the admin role (choose ONE option below)

-- OPTION 1: Add ROLE_ADMIN if missing (RECOMMENDED - Quick Fix)
INSERT INTO user_roles (user_id, role) 
SELECT id, 'ROLE_ADMIN' FROM users WHERE username = 'admin' 
AND NOT EXISTS (
    SELECT 1 FROM user_roles 
    WHERE user_id = (SELECT id FROM users WHERE username = 'admin') 
    AND role = 'ROLE_ADMIN'
);

-- OPTION 2: Delete and recreate admin user (if Option 1 doesn't work)
-- DELETE FROM user_roles WHERE user_id = (SELECT id FROM users WHERE username = 'admin');
-- DELETE FROM users WHERE username = 'admin';
-- Then restart the Spring Boot application

-- Step 3: Verify the fix
SELECT u.id, u.username, u.email, ur.role 
FROM users u 
LEFT JOIN user_roles ur ON u.id = ur.user_id 
WHERE u.username = 'admin';
