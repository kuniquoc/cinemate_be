-- V1__create_customer_profile.sql

-- Tạo ENUM type cho giới tính
CREATE TYPE gender_enum AS ENUM ('MALE', 'FEMALE', 'OTHER');

CREATE TABLE customers (
   id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
   account_id         UUID NOT NULL,  -- mapping sang auth-service.users.id
    first_name      VARCHAR(50),
    last_name       VARCHAR(50),
   avatar_url      VARCHAR(255),
   date_of_birth   DATE,
   gender          gender_enum DEFAULT 'OTHER',
   display_lang    VARCHAR(10) DEFAULT 'en',

   is_anonymous    BOOLEAN DEFAULT FALSE,

   created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   deleted_at      TIMESTAMP NULL
);