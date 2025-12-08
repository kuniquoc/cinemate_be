-- Create table "watch_history"
CREATE TABLE watch_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    movie_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    last_watched_position BIGINT NOT NULL DEFAULT 0,
    total_duration BIGINT NOT NULL DEFAULT 0,
    progress_percent DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,

    CONSTRAINT fk_watch_history_movie FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    CONSTRAINT uk_watch_history_movie_customer UNIQUE (movie_id, customer_id)
);

-- Create trigger to automatically update updated_at on UPDATE
CREATE TRIGGER update_watch_history_updated_at
    BEFORE UPDATE ON watch_history
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add indexes for better performance
CREATE INDEX idx_watch_history_movie_id ON watch_history(movie_id);
CREATE INDEX idx_watch_history_customer_id ON watch_history(customer_id);
CREATE INDEX idx_watch_history_updated_at ON watch_history(updated_at);
CREATE INDEX idx_watch_history_customer_updated ON watch_history(customer_id, updated_at DESC);
