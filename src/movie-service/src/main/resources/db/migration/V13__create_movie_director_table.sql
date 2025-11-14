-- Create junction table for movie-director relationship
CREATE TABLE movie_director (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    movie_id UUID NOT NULL,
    director_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    
    CONSTRAINT fk_movie_director_movie FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    CONSTRAINT fk_movie_director_director FOREIGN KEY (director_id) REFERENCES director(id) ON DELETE CASCADE,
    CONSTRAINT uk_movie_director UNIQUE (movie_id, director_id)
);

-- Create trigger to automatically update updated_at on UPDATE
CREATE TRIGGER update_movie_director_updated_at
    BEFORE UPDATE ON movie_director
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add indexes for better performance
CREATE INDEX idx_movie_director_movie_id ON movie_director(movie_id);
CREATE INDEX idx_movie_director_director_id ON movie_director(director_id);
