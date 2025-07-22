CREATE OR REPLACE VIEW view_users_exclude_owner_admin AS
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
    GROUP_CONCAT(ur.role) AS roles,
    u.created_at,
    u.updated_at,
    u.enabled
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
WHERE u.id NOT IN (SELECT user_id FROM user_roles WHERE role = 'OWNER' OR role = 'ADMIN')
GROUP BY u.id;