-- V3__change_duration_to_bigint.sql
-- Cập nhật cột duration sang kiểu bigint (Long)
-- Flyway migration
BEGIN;
ALTER TABLE movies
    ALTER COLUMN duration TYPE bigint
    USING duration::bigint;
COMMIT;
