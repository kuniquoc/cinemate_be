-- V3__create_favorites.sql
CREATE TABLE favorites
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    movie_id    UUID NOT NULL,
    created_at  TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_customer FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_favorites_customer_movie ON favorites (customer_id, movie_id);
