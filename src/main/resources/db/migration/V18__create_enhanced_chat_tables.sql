-- Create user_encryption_keys table for end-to-end encryption
CREATE TABLE user_encryption_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    public_key TEXT NOT NULL,
    private_key TEXT NOT NULL,
    key_fingerprint VARCHAR(255) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    key_version BIGINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_user_encryption_keys_user_id (user_id)
);

-- Create chat_files table for file sharing
CREATE TABLE chat_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    cloudinary_public_id VARCHAR(255) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    thumbnail_url VARCHAR(500),
    duration INT,
    width INT,
    height INT,
    chat_message_id BIGINT NOT NULL,
    uploaded_by BIGINT NOT NULL,
    uploaded_at DATETIME NOT NULL,
    encrypted BOOLEAN DEFAULT FALSE,
    encryption_key TEXT,
    active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (chat_message_id) REFERENCES chat_messages(id),
    FOREIGN KEY (uploaded_by) REFERENCES users(id)
);

-- Create user_devices table for push notifications
CREATE TABLE user_devices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_id VARCHAR(255) NOT NULL,
    fcm_token TEXT NOT NULL,
    device_type VARCHAR(20) NOT NULL,
    device_model VARCHAR(255) NOT NULL,
    operating_system VARCHAR(100) NOT NULL,
    app_version VARCHAR(50) NOT NULL,
    last_seen_at DATETIME NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    push_enabled BOOLEAN DEFAULT TRUE,
    chat_notifications BOOLEAN DEFAULT TRUE,
    friend_request_notifications BOOLEAN DEFAULT TRUE,
    system_notifications BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_user_devices_device_id (device_id)
);

-- Create message_reactions table for message reactions
CREATE TABLE message_reactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reaction VARCHAR(50) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES chat_messages(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_message_reactions_message_user_reaction (message_id, user_id, reaction)
);

-- Create pinned_messages table for pinned messages
CREATE TABLE pinned_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT NOT NULL,
    chat_room_id BIGINT NOT NULL,
    pinned_by BIGINT NOT NULL,
    pinned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES chat_messages(id),
    FOREIGN KEY (chat_room_id) REFERENCES chat_rooms(id),
    FOREIGN KEY (pinned_by) REFERENCES users(id),
    UNIQUE KEY uk_pinned_messages_message_id (message_id)
);

-- Create indexes for better performance
CREATE INDEX idx_user_encryption_keys_user_id ON user_encryption_keys(user_id);
CREATE INDEX idx_user_encryption_keys_active ON user_encryption_keys(active);

CREATE INDEX idx_chat_files_chat_message_id ON chat_files(chat_message_id);
CREATE INDEX idx_chat_files_uploaded_by ON chat_files(uploaded_by);
CREATE INDEX idx_chat_files_file_type ON chat_files(file_type);
CREATE INDEX idx_chat_files_active ON chat_files(active);

CREATE INDEX idx_user_devices_user_id ON user_devices(user_id);
CREATE INDEX idx_user_devices_device_id ON user_devices(device_id);
CREATE INDEX idx_user_devices_fcm_token ON user_devices(fcm_token);
CREATE INDEX idx_user_devices_active ON user_devices(active);

CREATE INDEX idx_message_reactions_message_id ON message_reactions(message_id);
CREATE INDEX idx_message_reactions_user_id ON message_reactions(user_id);

CREATE INDEX idx_pinned_messages_chat_room_id ON pinned_messages(chat_room_id);
CREATE INDEX idx_pinned_messages_pinned_by ON pinned_messages(pinned_by);
