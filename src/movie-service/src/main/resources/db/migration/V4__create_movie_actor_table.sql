-- Create junction table for movie-actor relationship
CREATE TABLE movie_actor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    movie_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,

    CONSTRAINT fk_movie_actor_movie FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    CONSTRAINT fk_movie_actor_actor FOREIGN KEY (actor_id) REFERENCES actor(id) ON DELETE CASCADE,
    CONSTRAINT uk_movie_actor UNIQUE (movie_id, actor_id)
);

-- Create trigger to automatically update updated_at on UPDATE
CREATE TRIGGER update_movie_actor_updated_at
    BEFORE UPDATE ON movie_actor
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add indexes for better performance
CREATE INDEX idx_movie_actor_movie_id ON movie_actor(movie_id);
CREATE INDEX idx_movie_actor_actor_id ON movie_actor(actor_id);
