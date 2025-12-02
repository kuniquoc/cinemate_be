CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    duration_days INTEGER NOT NULL,
    max_devices INTEGER NOT NULL DEFAULT 4,
    features JSONB,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on active plans
CREATE INDEX idx_subscription_plans_active ON subscription_plans(is_active);

-- Insert default Premium plan
INSERT INTO subscription_plans (name, description, price, duration_days, max_devices, features, is_active)
VALUES (
    'Premium',
    'Premium monthly subscription with unlimited access to all content',
    79000,
    30,
    4,
    '{"hd_streaming": true, "offline_download": true, "multiple_devices": true, "ad_free": true}'::jsonb,
    true
);
