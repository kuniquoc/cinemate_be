-- Database initialization script for interaction_db
-- Run this script to create all required tables

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Interaction events table (raw events)
CREATE TABLE IF NOT EXISTS interaction_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    user_id UUID NOT NULL,
    movie_id UUID,
    event_type VARCHAR(50) NOT NULL,
    event_data JSONB,
    client_timestamp TIMESTAMPTZ,
    server_timestamp TIMESTAMPTZ DEFAULT now() NOT NULL
);

-- Indexes for interaction_events
CREATE INDEX IF NOT EXISTS idx_events_user ON interaction_events(user_id);
CREATE INDEX IF NOT EXISTS idx_events_movie ON interaction_events(movie_id);
CREATE INDEX IF NOT EXISTS idx_events_type ON interaction_events(event_type);
CREATE INDEX IF NOT EXISTS idx_events_user_type ON interaction_events(user_id, event_type);
CREATE INDEX IF NOT EXISTS idx_events_movie_type ON interaction_events(movie_id, event_type);
CREATE INDEX IF NOT EXISTS idx_events_server_ts ON interaction_events(server_timestamp);

-- User features table (materialized features snapshot)
CREATE TABLE IF NOT EXISTS user_features (
    user_id UUID PRIMARY KEY,
    features JSONB NOT NULL DEFAULT '{}',
    version VARCHAR(50),
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL
);

-- Audit events table (processing audit trail)
CREATE TABLE IF NOT EXISTS audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL REFERENCES interaction_events(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    message TEXT,
    processed_at TIMESTAMPTZ DEFAULT now() NOT NULL
);

-- Indexes for audit_events
CREATE INDEX IF NOT EXISTS idx_audit_event_id ON audit_events(event_id);
CREATE INDEX IF NOT EXISTS idx_audit_status ON audit_events(status);

-- Model feedback table (for model improvement)
CREATE TABLE IF NOT EXISTS model_feedback (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    model_version VARCHAR(100) NOT NULL,
    impression_list JSONB NOT NULL,
    clicked_item_id UUID,
    watch_time_sec VARCHAR(20),
    context VARCHAR(50),
    feedback_timestamp TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL
);

-- Indexes for model_feedback
CREATE INDEX IF NOT EXISTS idx_feedback_user ON model_feedback(user_id);
CREATE INDEX IF NOT EXISTS idx_feedback_model ON model_feedback(model_version);
CREATE INDEX IF NOT EXISTS idx_feedback_timestamp ON model_feedback(feedback_timestamp);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger for user_features updated_at
DROP TRIGGER IF EXISTS update_user_features_updated_at ON user_features;
CREATE TRIGGER update_user_features_updated_at
    BEFORE UPDATE ON user_features
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Comments for documentation
COMMENT ON TABLE interaction_events IS 'Raw user interaction events (watch, search, rating, favorite)';
COMMENT ON TABLE user_features IS 'Computed user features for recommendation model';
COMMENT ON TABLE audit_events IS 'Audit trail for event processing';
COMMENT ON TABLE model_feedback IS 'User feedback on recommendations for model improvement';

-- Grant permissions (adjust user as needed)
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO admin;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO admin;
