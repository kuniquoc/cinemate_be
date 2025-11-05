-- Create user_devices table
CREATE TABLE user_devices
(
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL,
    device_name     VARCHAR(255) NOT NULL,
    device_type     VARCHAR(50),
    device_os       VARCHAR(100),
    browser         VARCHAR(100),
    ip_address      VARCHAR(45),
    user_agent      TEXT,
    is_current      BOOLEAN   DEFAULT FALSE,
    last_active_at  TIMESTAMP DEFAULT NOW(),
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    deleted_at      TIMESTAMP,
    CONSTRAINT fk_user_device FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Create index for faster queries
CREATE INDEX idx_user_devices_user_id ON user_devices (user_id);
CREATE INDEX idx_user_devices_last_active ON user_devices (last_active_at);
CREATE INDEX idx_user_devices_is_current ON user_devices (is_current);
