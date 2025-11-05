-- Thiết lập timezone (Asia/Saigon cũ đã deprecated, nên dùng Ho_Chi_Minh)
SET TIME ZONE 'Asia/Ho_Chi_Minh';

-- Extension để sinh UUID (cần cho Postgres)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE movie_categories (
      id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
      movie_id UUID NOT NULL,
      category_id UUID NOT NULL,
      created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
      FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
      FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
      UNIQUE (movie_id, category_id)
);

CREATE INDEX idx_movie_categories_movie_id ON movie_categories(movie_id);
CREATE INDEX idx_movie_categories_category_id ON movie_categories(category_id);
