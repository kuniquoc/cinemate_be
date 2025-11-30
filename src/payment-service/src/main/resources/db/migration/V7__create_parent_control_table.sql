-- Create parent_control table
CREATE TABLE parent_control (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id UUID NOT NULL,
    kid_id UUID NOT NULL,
    subscription_id UUID NOT NULL,
    blocked_categories TEXT,
    watch_time_limit_minutes INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_parent_control_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE CASCADE,
    CONSTRAINT unique_parent_kid UNIQUE (parent_id, kid_id)
);

-- Create indexes
CREATE INDEX idx_parent_control_parent ON parent_control(parent_id);
CREATE INDEX idx_parent_control_kid ON parent_control(kid_id);
CREATE INDEX idx_parent_control_subscription ON parent_control(subscription_id);
