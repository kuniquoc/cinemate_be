-- Flyway migration V6: add additional movie columns
ALTER TABLE movies
    ADD COLUMN IF NOT EXISTS vertical_poster VARCHAR(512),
    ADD COLUMN IF NOT EXISTS horizontal_poster VARCHAR(512),
    ADD COLUMN IF NOT EXISTS release_date DATE,
    ADD COLUMN IF NOT EXISTS trailer_url VARCHAR(512),
    ADD COLUMN IF NOT EXISTS age INTEGER,
    ADD COLUMN IF NOT EXISTS year INTEGER,
    ADD COLUMN IF NOT EXISTS country VARCHAR(100);

