CREATE TABLE devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    device_name VARCHAR(255) NOT NULL,
    device_type VARCHAR(20) NOT NULL,
    device_id VARCHAR(255) NOT NULL,
    browser_info VARCHAR(255),
    os_info VARCHAR(255),
    ip_address VARCHAR(45),
    last_accessed TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_user_device UNIQUE (user_id, device_id)
);

-- Create indexes
CREATE INDEX idx_devices_user_id ON devices(user_id);
CREATE INDEX idx_devices_user_active ON devices(user_id, is_active);
CREATE INDEX idx_devices_device_id ON devices(device_id);

-- Add check constraint for device type
ALTER TABLE devices ADD CONSTRAINT chk_device_type 
    CHECK (device_type IN ('WEB', 'MOBILE', 'TABLET', 'TV', 'DESKTOP'));
