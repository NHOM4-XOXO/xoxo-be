CREATE OR REPLACE VIEW view_users_exclude_owner_admin AS
SELECT u.*
FROM users u
WHERE u.id NOT IN (
    SELECT user_id FROM user_roles WHERE role = 'OWNER'
       OR role = 'ADMIN'
);