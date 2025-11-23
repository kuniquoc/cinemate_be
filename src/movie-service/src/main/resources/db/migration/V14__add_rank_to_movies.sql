-- Add rank column to movies table
ALTER TABLE movies ADD COLUMN rank INTEGER;

-- Calculate and assign ranks to existing movies based on their average review stars
-- Movies with higher average stars get better (lower) rank numbers
WITH movie_ratings AS (
    SELECT 
        m.id,
        COALESCE(AVG(r.stars), 0) as avg_rating,
        COUNT(r.id) as review_count
    FROM movies m
    LEFT JOIN review r ON r.movie_id = m.id AND r.deleted_at IS NULL
    GROUP BY m.id
),
ranked_movies AS (
    SELECT 
        id,
        ROW_NUMBER() OVER (ORDER BY avg_rating DESC, review_count DESC, id) as calculated_rank
    FROM movie_ratings
)
UPDATE movies
SET rank = ranked_movies.calculated_rank
FROM ranked_movies
WHERE movies.id = ranked_movies.id;

-- Add index on rank for better performance when sorting
CREATE INDEX idx_movies_rank ON movies(rank ASC);

-- Add comment to describe the column
COMMENT ON COLUMN movies.rank IS 'Movie ranking position (1=best) based on average review stars, calculated daily by scheduler';
