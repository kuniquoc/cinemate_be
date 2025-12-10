-- V17: Delete old movie seed data from V8 migration
-- The old data used deprecated status values (READY, PROCESSING) and qualities_json format
-- This migration cleans up that data to allow fresh seeding with correct enum values

DELETE FROM movies
WHERE title IN (
    'Interstellar',
    'Inception',
    'Parasite',
    'The Dark Knight',
    'Dune: Part Two',
    'Oppenheimer',
    'Your Name',
    'Spirited Away',
    'The Godfather',
    'Train to Busan'
);
