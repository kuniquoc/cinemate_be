-- Create family_invitations table
CREATE TABLE family_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL,
    invitation_token VARCHAR(255) NOT NULL UNIQUE,
    mode VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    invited_by UUID NOT NULL,
    invited_user_id UUID,
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invitation_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE CASCADE,
    CONSTRAINT chk_invitation_mode CHECK (mode IN ('ADULT', 'KID')),
    CONSTRAINT chk_invitation_status CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'CANCELLED'))
);

-- Create indexes
CREATE INDEX idx_family_invitations_subscription ON family_invitations(subscription_id);
CREATE INDEX idx_family_invitations_token ON family_invitations(invitation_token);
CREATE INDEX idx_family_invitations_status ON family_invitations(subscription_id, status);
CREATE INDEX idx_family_invitations_expires ON family_invitations(expires_at, status);
