-- V3: Fix uuid_generate_v7 function
-- The previous function was generating incorrect UUID strings

CREATE OR REPLACE FUNCTION uuid_generate_v7() RETURNS uuid AS $$
BEGIN
    RETURN (
        lpad(to_hex((extract(epoch from clock_timestamp()) * 1000)::bigint), 12, '0') ||
        '7' ||
        substring(encode(gen_random_bytes(10), 'hex') from 1 for 19)
    )::uuid;
END;
$$ LANGUAGE plpgsql;