CREATE TABLE customers
(
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id    UUID NOT NULL UNIQUE,
    first_name    VARCHAR(50),
    last_name     VARCHAR(50),
    avatar_url    VARCHAR(255),
    date_of_birth DATE,
    gender        VARCHAR(10)      DEFAULT 'OTHER',
    display_lang  VARCHAR(10)      DEFAULT 'en',
    is_anonymous  BOOLEAN          DEFAULT FALSE,
    created_at    TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP NULL
);

CREATE TABLE favorites
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    movie_id    UUID NOT NULL,
    created_at  TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP        DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP NULL,
    CONSTRAINT fk_customer_account FOREIGN KEY (customer_id) REFERENCES customers (account_id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_favorites_customer_movie ON favorites (customer_id, movie_id);