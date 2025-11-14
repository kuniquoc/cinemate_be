-- Change qualities_json from JSONB to TEXT[] array
-- This migration changes the storage format from JSON with paths to a simple array of quality names

-- Step 1: Add new column with TEXT[] type
ALTER TABLE movies ADD COLUMN qualities TEXT[];

-- Step 2: Migrate existing data - extract quality names from JSON keys
UPDATE movies
SET qualities = ARRAY(
    SELECT jsonb_object_keys(qualities_json)
    WHERE qualities_json IS NOT NULL
)
WHERE qualities_json IS NOT NULL;

-- Step 3: Drop old column
ALTER TABLE movies DROP COLUMN qualities_json;

-- Step 4: Create index on qualities for better query performance
CREATE INDEX idx_movies_qualities ON movies USING GIN(qualities);
