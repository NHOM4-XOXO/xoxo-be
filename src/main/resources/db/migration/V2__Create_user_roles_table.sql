CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role ENUM('OWNER', 'ADMIN', 'USER') NOT NULL,
    PRIMARY KEY (user_id, role),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
-- Tạo index cho user_id
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role);