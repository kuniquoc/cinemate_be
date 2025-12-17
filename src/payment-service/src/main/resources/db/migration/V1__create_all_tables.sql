CREATE
EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create subscription_plans table
CREATE TABLE subscription_plans
(
    id            UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    name          VARCHAR(100)   NOT NULL,
    description   TEXT,
    price         DECIMAL(10, 2) NOT NULL,
    duration_days INTEGER        NOT NULL,
    max_devices   INTEGER        NOT NULL DEFAULT 4,
    features      JSONB,
    is_active     BOOLEAN        NOT NULL DEFAULT true,
    max_members   INTEGER,
    is_family_plan BOOLEAN       NOT NULL DEFAULT false,
    created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP
);

-- Create subscriptions table
CREATE TABLE subscriptions
(
    id         UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL,
    plan_id    UUID        NOT NULL,
    status     VARCHAR(20) NOT NULL,
    start_date TIMESTAMP,
    end_date   TIMESTAMP,
    auto_renew BOOLEAN     NOT NULL DEFAULT false,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_subscription_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans (id),
    CONSTRAINT chk_subscription_status CHECK (status IN ('PENDING', 'ACTIVE', 'EXPIRED', 'CANCELLED'))
);

-- Create payments table
CREATE TABLE payments
(
    id                 UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
    user_id            UUID           NOT NULL,
    subscription_id    UUID,
    amount             DECIMAL(10, 2) NOT NULL,
    payment_method     VARCHAR(20)    NOT NULL,
    status             VARCHAR(20)    NOT NULL,
    transaction_id     VARCHAR(255) UNIQUE,
    vnp_txn_ref        VARCHAR(100) UNIQUE,
    vnp_transaction_no VARCHAR(100),
    vnp_bank_code      VARCHAR(20),
    vnp_card_type      VARCHAR(20),
    vnp_order_info     TEXT,
    vnp_pay_date       VARCHAR(14),
    vnp_response_code  VARCHAR(2),
    payment_date       TIMESTAMP,
    user_email         VARCHAR(255),
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at         TIMESTAMP,
    CONSTRAINT fk_payment_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions (id),
    CONSTRAINT chk_payment_method CHECK (payment_method IN ('VNPAY', 'MOMO', 'ZALOPAY', 'BANK_TRANSFER', 'CREDIT_CARD')),
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED', 'REFUNDED'))
);

-- Create devices table
CREATE TABLE devices
(
    id            UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL,
    device_name   VARCHAR(255) NOT NULL,
    device_type   VARCHAR(20)  NOT NULL,
    device_id     VARCHAR(255) NOT NULL,
    browser_info  VARCHAR(255),
    os_info       VARCHAR(255),
    ip_address    VARCHAR(45),
    last_accessed TIMESTAMP,
    is_active     BOOLEAN      NOT NULL DEFAULT true,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP,
    CONSTRAINT unique_user_device UNIQUE (user_id, device_id),
    CONSTRAINT chk_device_type CHECK (device_type IN ('WEB', 'MOBILE', 'TABLET', 'TV', 'DESKTOP'))
);

-- Create family_members table
CREATE TABLE family_members
(
    id              UUID PRIMARY KEY   DEFAULT gen_random_uuid(),
    subscription_id UUID      NOT NULL,
    user_id         UUID      NOT NULL,
    is_owner        BOOLEAN   NOT NULL DEFAULT false,
    joined_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP,
    CONSTRAINT fk_family_member_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions (id) ON DELETE CASCADE,
    CONSTRAINT unique_subscription_user UNIQUE (subscription_id, user_id)
);

-- Create parent_control table
CREATE TABLE parent_control
(
    id                       UUID PRIMARY KEY   DEFAULT gen_random_uuid(),
    parent_id                UUID      NOT NULL,
    kid_id                   UUID      NOT NULL,
    subscription_id          UUID      NOT NULL,
    blocked_categories       TEXT,
    watch_time_limit_minutes INTEGER,
    created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at               TIMESTAMP,
    CONSTRAINT fk_parent_control_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions (id) ON DELETE CASCADE,
    CONSTRAINT unique_parent_kid UNIQUE (parent_id, kid_id)
);

-- Create family_invitations table
CREATE TABLE family_invitations
(
    id               UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    subscription_id  UUID         NOT NULL,
    invitation_token VARCHAR(255) NOT NULL UNIQUE,
    mode             VARCHAR(10)  NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    invited_by       UUID         NOT NULL,
    invited_user_id  UUID,
    recipient_email  VARCHAR(255),
    expires_at       TIMESTAMP    NOT NULL,
    accepted_at      TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP,
    CONSTRAINT fk_invitation_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions (id) ON DELETE CASCADE,
    CONSTRAINT chk_invitation_mode CHECK (mode IN ('ADULT', 'KID')),
    CONSTRAINT chk_invitation_status CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'CANCELLED'))
);

-- Create indexes
CREATE INDEX idx_subscription_plans_active ON subscription_plans (is_active);
CREATE INDEX idx_subscription_plans_family ON subscription_plans (is_family_plan, is_active);

CREATE INDEX idx_subscriptions_user_id ON subscriptions (user_id);
CREATE INDEX idx_subscriptions_status ON subscriptions (status);
CREATE INDEX idx_subscriptions_end_date ON subscriptions (end_date);
CREATE INDEX idx_subscriptions_user_status ON subscriptions (user_id, status);

CREATE INDEX idx_payments_user_id ON payments (user_id);
CREATE INDEX idx_payments_subscription_id ON payments (subscription_id);
CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_transaction_id ON payments (transaction_id);
CREATE INDEX idx_payments_vnp_txn_ref ON payments (vnp_txn_ref);
CREATE INDEX idx_payments_payment_date ON payments (payment_date);
CREATE INDEX idx_payments_user_email ON payments (user_email);

CREATE INDEX idx_devices_user_id ON devices (user_id);
CREATE INDEX idx_devices_user_active ON devices (user_id, is_active);
CREATE INDEX idx_devices_device_id ON devices (device_id);

CREATE INDEX idx_family_members_subscription ON family_members (subscription_id);
CREATE INDEX idx_family_members_user ON family_members (user_id);
CREATE INDEX idx_family_members_owner ON family_members (subscription_id, is_owner);

CREATE INDEX idx_parent_control_parent ON parent_control (parent_id);
CREATE INDEX idx_parent_control_kid ON parent_control (kid_id);
CREATE INDEX idx_parent_control_subscription ON parent_control (subscription_id);

CREATE INDEX idx_family_invitations_subscription ON family_invitations (subscription_id);
CREATE INDEX idx_family_invitations_token ON family_invitations (invitation_token);
CREATE INDEX idx_family_invitations_status ON family_invitations (subscription_id, status);
CREATE INDEX idx_family_invitations_expires ON family_invitations (expires_at, status);