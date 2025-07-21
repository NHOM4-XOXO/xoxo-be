CREATE OR REPLACE VIEW view_users_exclude_owner AS
SELECT 
    u.id,
    u.email,
    u.first_name,
    u.last_name,
    u.date_of_birth,
    u.gender,
    u.avatar_url,
    u.cover_url,
    u.bio,
    u.enabled,
    u.created_at,
    u.updated_at
FROM users u
WHERE u.id NOT IN (
    SELECT user_id FROM user_roles WHERE role = 'OWNER'
);