-- Add process_status column to movies table
ALTER TABLE movies
ADD COLUMN process_status VARCHAR(50) CHECK (process_status IN ('UPLOADING', 'PROCESSING', 'COMPLETED', 'FAILED'));

-- Migrate existing status data to process_status
-- Old status values (PENDING, PROCESSING, READY, FAILED) -> process_status
-- PENDING -> UPLOADING
-- PROCESSING -> PROCESSING  
-- READY -> COMPLETED
-- FAILED -> FAILED
UPDATE movies
SET process_status = CASE
    WHEN status = 'PENDING' THEN 'UPLOADING'
    WHEN status = 'PROCESSING' THEN 'PROCESSING'
    WHEN status = 'READY' THEN 'COMPLETED'
    WHEN status = 'FAILED' THEN 'FAILED'
    ELSE 'UPLOADING'
END;

-- Make process_status NOT NULL after data migration
ALTER TABLE movies
ALTER COLUMN process_status SET NOT NULL;

-- Update status column constraint to new values (DRAFT, PRIVATE, PUBLIC)
ALTER TABLE movies
DROP CONSTRAINT IF EXISTS movies_status_check;

-- Set all existing movies to DRAFT status (they will be reviewed and published later)
UPDATE movies
SET status = CASE
    WHEN status IN ('PENDING', 'PROCESSING', 'FAILED') THEN 'DRAFT'
    WHEN status = 'READY' THEN 'PRIVATE'
    ELSE 'DRAFT'
END;

ALTER TABLE movies
ADD CONSTRAINT movies_status_check CHECK (status IN ('DRAFT', 'PRIVATE', 'PUBLIC'));



-- Create index on process_status for better query performance
CREATE INDEX idx_movies_process_status ON movies(process_status);

-- Update the existing status index comment
COMMENT ON INDEX idx_movies_status IS 'Index for movie publication status (DRAFT, PRIVATE, PUBLIC)';
COMMENT ON INDEX idx_movies_process_status IS 'Index for movie processing status (UPLOADING, PROCESSING, COMPLETED, FAILED)';
