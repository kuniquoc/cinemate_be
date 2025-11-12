-- Add movie_id column
ALTER TABLE chunk_uploads ADD COLUMN movie_id UUID NOT NULL;

-- Add foreign key constraint
ALTER TABLE chunk_uploads ADD CONSTRAINT fk_chunk_uploads_movie 
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE;

-- Drop old columns that stored movie metadata
ALTER TABLE chunk_uploads DROP COLUMN IF EXISTS movie_title;
ALTER TABLE chunk_uploads DROP COLUMN IF EXISTS movie_description;

-- Create index for better performance on foreign key lookups
CREATE INDEX idx_chunk_uploads_movie_id ON chunk_uploads(movie_id);
