-- Flyway migration V1: Create all tables with final schema
-- This migration consolidates all previous migrations into a single, optimized version

-- Set timezone
SET TIME ZONE 'Asia/Ho_Chi_Minh';

-- Extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Trigger function for updating updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';

-- Create movies table
CREATE TABLE movies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL CHECK (status IN ('DRAFT', 'PRIVATE', 'PUBLIC')),
    process_status VARCHAR(50) NOT NULL CHECK (process_status IN ('UPLOADING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    qualities TEXT[],
    vertical_poster VARCHAR(512),
    horizontal_poster VARCHAR(512),
    release_date DATE,
    trailer_url VARCHAR(512),
    age INTEGER,
    year INTEGER,
    country VARCHAR(100),
    is_vip BOOLEAN NOT NULL DEFAULT FALSE,
    rank INTEGER
);

-- Create indexes for movies
CREATE INDEX idx_movies_status ON movies (status);
CREATE INDEX idx_movies_created_at ON movies (created_at);
CREATE INDEX idx_movies_title ON movies (title);
CREATE INDEX idx_movies_qualities ON movies USING GIN (qualities);
CREATE INDEX idx_movies_process_status ON movies (process_status);
CREATE INDEX idx_movies_rank ON movies (rank ASC);

-- Create trigger for movies
CREATE TRIGGER update_movies_updated_at
    BEFORE UPDATE ON movies
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create chunk_uploads table
CREATE TABLE chunk_uploads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    upload_id VARCHAR(255) NOT NULL UNIQUE,
    filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    total_size BIGINT NOT NULL,
    total_chunks INTEGER NOT NULL,
    chunk_size INTEGER NOT NULL,
    uploaded_chunks INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    uploaded_chunks_list TEXT DEFAULT '[]',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    movie_id UUID NOT NULL,
    CONSTRAINT fk_chunk_uploads_movie FOREIGN KEY (movie_id) REFERENCES movies (id) ON DELETE CASCADE,
    CONSTRAINT chk_chunk_uploads_status CHECK (status IN ('INITIATED', 'IN_PROGRESS', 'COMPLETED', 'MERGING', 'FAILED', 'EXPIRED')),
    CONSTRAINT chk_chunk_uploads_total_size CHECK (total_size > 0),
    CONSTRAINT chk_chunk_uploads_total_chunks CHECK (total_chunks > 0),
    CONSTRAINT chk_chunk_uploads_chunk_size CHECK (chunk_size > 0),
    CONSTRAINT chk_chunk_uploads_uploaded_chunks CHECK (uploaded_chunks >= 0 AND uploaded_chunks <= total_chunks)
);

-- Create indexes for chunk_uploads
CREATE INDEX idx_chunk_uploads_upload_id ON chunk_uploads (upload_id);
CREATE INDEX idx_chunk_uploads_status ON chunk_uploads (status);
CREATE INDEX idx_chunk_uploads_expires_at ON chunk_uploads (expires_at);
CREATE INDEX idx_chunk_uploads_created_at ON chunk_uploads (created_at);
CREATE INDEX idx_chunk_uploads_movie_id ON chunk_uploads (movie_id);

-- Create trigger for chunk_uploads
CREATE TRIGGER update_chunk_uploads_updated_at
    BEFORE UPDATE ON chunk_uploads
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create actor table
CREATE TABLE actor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fullname VARCHAR(255) NOT NULL,
    biography TEXT,
    avatar VARCHAR(512),
    date_of_birth DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Create trigger for actor
CREATE TRIGGER update_actor_updated_at
    BEFORE UPDATE ON actor
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create movie_actor table
CREATE TABLE movie_actor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    movie_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_movie_actor_movie FOREIGN KEY (movie_id) REFERENCES movies (id) ON DELETE CASCADE,
    CONSTRAINT fk_movie_actor_actor FOREIGN KEY (actor_id) REFERENCES actor (id) ON DELETE CASCADE,
    CONSTRAINT uk_movie_actor UNIQUE (movie_id, actor_id)
);

-- Create indexes for movie_actor
CREATE INDEX idx_movie_actor_movie_id ON movie_actor (movie_id);
CREATE INDEX idx_movie_actor_actor_id ON movie_actor (actor_id);

-- Create trigger for movie_actor
CREATE TRIGGER update_movie_actor_updated_at
    BEFORE UPDATE ON movie_actor
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create review table
CREATE TABLE review (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    movie_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    content TEXT NOT NULL,
    stars INTEGER NOT NULL CHECK (stars >= 1 AND stars <= 5),
    user_name VARCHAR(255) NOT NULL,
    user_avatar VARCHAR(512),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_review_movie FOREIGN KEY (movie_id) REFERENCES movies (id) ON DELETE CASCADE,
    CONSTRAINT uk_review_movie_customer UNIQUE (movie_id, customer_id)
);

-- Create indexes for review
CREATE INDEX idx_review_movie_id ON review (movie_id);
CREATE INDEX idx_review_customer_id ON review (customer_id);
CREATE INDEX idx_review_stars ON review (stars);

-- Create trigger for review
CREATE TRIGGER update_review_updated_at
    BEFORE UPDATE ON review
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create categories table
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Create trigger for categories
CREATE TRIGGER update_categories_updated_at
    BEFORE UPDATE ON categories
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create movie_categories table
CREATE TABLE movie_categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    movie_id UUID NOT NULL,
    category_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    FOREIGN KEY (movie_id) REFERENCES movies (id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE CASCADE,
    UNIQUE (movie_id, category_id)
);

-- Create indexes for movie_categories
CREATE INDEX idx_movie_categories_movie_id ON movie_categories (movie_id);
CREATE INDEX idx_movie_categories_category_id ON movie_categories (category_id);

-- Create trigger for movie_categories
CREATE TRIGGER update_movie_categories_updated_at
    BEFORE UPDATE ON movie_categories
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create director table
CREATE TABLE director (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fullname VARCHAR(255) NOT NULL,
    biography TEXT,
    avatar VARCHAR(512),
    date_of_birth DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Create trigger for director
CREATE TRIGGER update_director_updated_at
    BEFORE UPDATE ON director
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create movie_director table
CREATE TABLE movie_director (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    movie_id UUID NOT NULL,
    director_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_movie_director_movie FOREIGN KEY (movie_id) REFERENCES movies (id) ON DELETE CASCADE,
    CONSTRAINT fk_movie_director_director FOREIGN KEY (director_id) REFERENCES director (id) ON DELETE CASCADE,
    CONSTRAINT uk_movie_director UNIQUE (movie_id, director_id)
);

-- Create indexes for movie_director
CREATE INDEX idx_movie_director_movie_id ON movie_director (movie_id);
CREATE INDEX idx_movie_director_director_id ON movie_director (director_id);

-- Create trigger for movie_director
CREATE TRIGGER update_movie_director_updated_at
    BEFORE UPDATE ON movie_director
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create watch_history table
CREATE TABLE watch_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    movie_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    last_watched_position BIGINT NOT NULL DEFAULT 0,
    total_duration BIGINT NOT NULL DEFAULT 0,
    progress_percent DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_watch_history_movie FOREIGN KEY (movie_id) REFERENCES movies (id) ON DELETE CASCADE,
    CONSTRAINT uk_watch_history_movie_customer UNIQUE (movie_id, customer_id)
);

-- Create indexes for watch_history
CREATE INDEX idx_watch_history_movie_id ON watch_history (movie_id);
CREATE INDEX idx_watch_history_customer_id ON watch_history (customer_id);
CREATE INDEX idx_watch_history_updated_at ON watch_history (updated_at);
CREATE INDEX idx_watch_history_customer_updated ON watch_history (customer_id, updated_at DESC);

-- Create trigger for watch_history
CREATE TRIGGER update_watch_history_updated_at
    BEFORE UPDATE ON watch_history
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add comments
COMMENT ON COLUMN movies.rank IS 'Movie ranking position (1=best) based on average review stars, calculated daily by scheduler';
COMMENT ON INDEX idx_movies_status IS 'Index for movie publication status (DRAFT, PRIVATE, PUBLIC)';
COMMENT ON INDEX idx_movies_process_status IS 'Index for movie processing status (UPLOADING, PROCESSING, COMPLETED, FAILED)';