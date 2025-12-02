-- Add family plan support to subscription_plans table
ALTER TABLE subscription_plans 
ADD COLUMN max_members INTEGER,
ADD COLUMN is_family_plan BOOLEAN NOT NULL DEFAULT false;

-- Update index to include family plans
CREATE INDEX idx_subscription_plans_family ON subscription_plans(is_family_plan, is_active);

-- Insert Family plan
INSERT INTO subscription_plans (name, description, price, duration_days, max_devices, max_members, is_family_plan, features, is_active)
VALUES (
    'Family',
    'Family plan with up to 6 members, perfect for families with parental controls',
    149000,
    30,
    10,
    6,
    true,
    '{"hd_streaming": true, "offline_download": true, "multiple_devices": true, "ad_free": true, "parental_controls": true, "family_sharing": true}'::jsonb,
    true
);
