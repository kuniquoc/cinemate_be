-- V2__alter_gender_to_string.sql

-- 1. Xóa cột gender cũ
ALTER TABLE customers DROP COLUMN gender;

-- 2. Thêm cột mới kiểu VARCHAR
ALTER TABLE customers ADD COLUMN gender VARCHAR(10) DEFAULT 'OTHER';
