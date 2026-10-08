--liquibase formatted sql

--changeset codex:019-pricing-plan-assessment-limit
ALTER TABLE pricing_plan
    ADD COLUMN IF NOT EXISTS monthly_assessment_limit INTEGER NOT NULL DEFAULT 20
    CHECK (monthly_assessment_limit > 0);

UPDATE pricing_plan
SET monthly_assessment_limit = CASE code
    WHEN 'FREE_TRIAL' THEN 20
    WHEN 'BASIC' THEN 100
    WHEN 'PREMIUM' THEN 300
    ELSE monthly_assessment_limit
END;

--changeset codex:019-assessment-quota-reservations
CREATE TABLE assessment_quota_reservation (
    id BIGSERIAL PRIMARY KEY,
    request_id UUID NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    plan_code VARCHAR(50) NOT NULL,
    cycle_start DATE NOT NULL,
    assessment_limit INTEGER NOT NULL CHECK (assessment_limit > 0),
    status VARCHAR(12) NOT NULL CHECK (status IN ('RESERVED', 'CONSUMED', 'RELEASED')),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_assessment_quota_reservation_usage
    ON assessment_quota_reservation (user_id, cycle_start, status);
