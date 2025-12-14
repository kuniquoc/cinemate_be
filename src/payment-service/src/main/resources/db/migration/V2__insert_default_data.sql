-- Insert default Premium plan
INSERT INTO subscription_plans (name, description, price, duration_days, max_devices, features, is_active)
VALUES ('Premium',
        'Premium monthly subscription with unlimited access to all content',
        79000,
        30,
        4,
        '{"hd_streaming": true, "offline_download": true, "multiple_devices": true, "ad_free": true}'::jsonb,
        true);

-- Insert Family plan
INSERT INTO subscription_plans (name, description, price, duration_days, max_devices, max_members, is_family_plan,
                                features, is_active)
VALUES ('Family',
        'Family plan with up to 6 members, perfect for families with parental controls',
        149000,
        30,
        10,
        6,
        true,
        '{"hd_streaming": true, "offline_download": true, "multiple_devices": true, "ad_free": true, "parental_controls": true, "family_sharing": true}'::jsonb,
        true);