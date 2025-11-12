-- Flyway migration V9: add is_vip column to movies table
ALTER TABLE movies
    ADD COLUMN IF NOT EXISTS is_vip BOOLEAN NOT NULL DEFAULT FALSE;
