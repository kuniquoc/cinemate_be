-- V4__update_favorite_fk.sql

-- Đảm bảo cột account_id có unique constraint
ALTER TABLE customers
    ADD CONSTRAINT uq_customers_account_id UNIQUE (account_id);

-- Xóa foreign key cũ (tham chiếu tới id)
ALTER TABLE favorites
DROP CONSTRAINT IF EXISTS fk_customer;

-- Thêm foreign key mới tham chiếu tới account_id
ALTER TABLE favorites
    ADD CONSTRAINT fk_customer_account
        FOREIGN KEY (customer_id)
            REFERENCES customers (account_id)
            ON DELETE CASCADE;
