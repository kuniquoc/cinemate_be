-- Add user_email column to payments table
ALTER TABLE payments ADD COLUMN user_email VARCHAR(255);

-- Create index for email lookups
CREATE INDEX idx_payments_user_email ON payments(user_email);

-- Add comment for documentation
COMMENT ON COLUMN payments.user_email IS 'Email address of the user making the payment, captured at payment creation time';
