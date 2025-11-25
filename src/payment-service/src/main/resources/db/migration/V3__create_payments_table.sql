CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    subscription_id BIGINT,
    amount DECIMAL(10, 2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    transaction_id VARCHAR(255) UNIQUE,
    vnp_txn_ref VARCHAR(100) UNIQUE,
    vnp_transaction_no VARCHAR(100),
    vnp_bank_code VARCHAR(20),
    vnp_card_type VARCHAR(20),
    vnp_order_info TEXT,
    vnp_pay_date VARCHAR(14),
    vnp_response_code VARCHAR(2),
    payment_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions(id)
);

-- Create indexes
CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_subscription_id ON payments(subscription_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_transaction_id ON payments(transaction_id);
CREATE INDEX idx_payments_vnp_txn_ref ON payments(vnp_txn_ref);
CREATE INDEX idx_payments_payment_date ON payments(payment_date);

-- Add check constraints
ALTER TABLE payments ADD CONSTRAINT chk_payment_method 
    CHECK (payment_method IN ('VNPAY', 'MOMO', 'ZALOPAY', 'BANK_TRANSFER', 'CREDIT_CARD'));

ALTER TABLE payments ADD CONSTRAINT chk_payment_status 
    CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED', 'REFUNDED'));
