-- Create table "review"
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

    CONSTRAINT fk_review_movie FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    CONSTRAINT uk_review_movie_customer UNIQUE (movie_id, customer_id)
);

-- Create trigger to automatically update updated_at on UPDATE
CREATE TRIGGER update_review_updated_at
    BEFORE UPDATE ON review
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add indexes for better performance
CREATE INDEX idx_review_movie_id ON review(movie_id);
CREATE INDEX idx_review_customer_id ON review(customer_id);
CREATE INDEX idx_review_stars ON review(stars);
