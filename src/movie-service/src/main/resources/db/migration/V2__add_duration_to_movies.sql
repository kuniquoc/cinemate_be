-- Add duration column to movies (seconds)
ALTER TABLE movies
    ADD COLUMN duration INTEGER;

-- Optional: add index for duration if queries will filter/sort by it
-- CREATE INDEX IF NOT EXISTS idx_movies_duration ON movies (duration);

COMMENT ON COLUMN movies.duration IS 'Duration of the movie in seconds (nullable)';
