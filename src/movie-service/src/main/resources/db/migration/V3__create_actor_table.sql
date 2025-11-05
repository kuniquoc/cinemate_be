-- Create table "actor"
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

-- Create trigger function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to automatically update updated_at on UPDATE
CREATE TRIGGER update_actor_updated_at
    BEFORE UPDATE ON actor
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

