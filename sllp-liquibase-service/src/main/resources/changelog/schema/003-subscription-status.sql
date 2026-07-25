--liquibase formatted sql

--changeset yasin:003-add-subscription-status
-- Add explicit status column to user_subscription for state machine (ACTIVE/EXPIRED/CANCELLED/PENDING)
ALTER TABLE user_subscription
    ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'
    CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED', 'PENDING'));

-- Backfill existing rows: any row whose end_date is in the past is EXPIRED, rest are ACTIVE
UPDATE user_subscription
SET status = CASE
    WHEN end_date IS NOT NULL AND end_date < CURRENT_DATE THEN 'EXPIRED'
    ELSE 'ACTIVE'
END;

CREATE INDEX idx_subscription_user_status ON user_subscription (user_id, status);

--changeset yasin:003-add-payment-reference
-- Placeholder column for Stripe (or other payment provider) transaction/charge ID
ALTER TABLE user_subscription
    ADD COLUMN payment_reference VARCHAR(255);
