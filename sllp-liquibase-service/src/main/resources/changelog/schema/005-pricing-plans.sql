--liquibase formatted sql

--changeset codex:005-create-pricing-plan
CREATE TABLE pricing_plan (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(120) NOT NULL,
    description TEXT,
    price_minor BIGINT NOT NULL CHECK (price_minor >= 0),
    currency CHAR(3) NOT NULL,
    billing_interval VARCHAR(20) NOT NULL DEFAULT 'MONTHLY' CHECK (billing_interval = 'MONTHLY'),
    duration_days INTEGER CHECK (duration_days IS NULL OR duration_days > 0),
    access_role VARCHAR(50) NOT NULL CHECK (access_role IN ('USER_FREE_TRIAL', 'USER_BASIC')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pricing_plan_feature (
    id BIGSERIAL PRIMARY KEY,
    pricing_plan_id BIGINT NOT NULL REFERENCES pricing_plan(id) ON DELETE RESTRICT,
    feature_text VARCHAR(500) NOT NULL,
    display_order INTEGER NOT NULL,
    CONSTRAINT uq_pricing_plan_feature_order UNIQUE (pricing_plan_id, display_order)
);
CREATE INDEX idx_pricing_plan_active_order ON pricing_plan (active, display_order);

--changeset codex:005-seed-pricing-plans
INSERT INTO pricing_plan (code, display_name, description, price_minor, currency, duration_days, access_role, active, display_order)
VALUES
  ('FREE_TRIAL', 'Free Trial', 'Get started with essential writing feedback.', 0, 'USD', NULL, 'USER_FREE_TRIAL', TRUE, 10),
  ('BASIC', 'Basic', 'More feedback and document support for regular learners.', 999, 'USD', 30, 'USER_BASIC', TRUE, 20),
  ('PREMIUM', 'Premium', 'Priority processing and advanced learning insights.', 2999, 'USD', 30, 'USER_BASIC', TRUE, 30)
ON CONFLICT (code) DO NOTHING;

INSERT INTO pricing_plan_feature (pricing_plan_id, feature_text, display_order)
SELECT p.id, v.feature_text, v.display_order
FROM pricing_plan p
JOIN (VALUES
 ('FREE_TRIAL', '20 compositions per month', 10), ('FREE_TRIAL', 'Basic AI feedback', 20), ('FREE_TRIAL', 'Standard support', 30),
 ('BASIC', 'Unlimited compositions', 10), ('BASIC', 'Detailed AI feedback', 20), ('BASIC', 'Document uploads', 30), ('BASIC', 'Standard support', 40),
 ('PREMIUM', 'Everything in Basic', 10), ('PREMIUM', 'Priority AI processing', 20), ('PREMIUM', 'Advanced analytics', 30), ('PREMIUM', 'Priority support', 40)
) AS v(code, feature_text, display_order) ON v.code = p.code
WHERE NOT EXISTS (SELECT 1 FROM pricing_plan_feature f WHERE f.pricing_plan_id = p.id AND f.display_order = v.display_order);

--changeset codex:005-snapshot-subscriptions
ALTER TABLE user_subscription ADD COLUMN pricing_plan_id BIGINT;
ALTER TABLE user_subscription ADD COLUMN plan_code_snapshot VARCHAR(50);
ALTER TABLE user_subscription ADD COLUMN plan_name_snapshot VARCHAR(120);
ALTER TABLE user_subscription ADD COLUMN price_minor_snapshot BIGINT;
ALTER TABLE user_subscription ADD COLUMN currency_snapshot CHAR(3);
ALTER TABLE user_subscription ADD COLUMN features_snapshot TEXT;

UPDATE user_subscription us
SET pricing_plan_id = p.id,
    plan_code_snapshot = p.code,
    plan_name_snapshot = p.display_name,
    price_minor_snapshot = p.price_minor,
    currency_snapshot = p.currency,
    features_snapshot = COALESCE((SELECT json_agg(f.feature_text ORDER BY f.display_order)::text FROM pricing_plan_feature f WHERE f.pricing_plan_id = p.id), '[]')
FROM pricing_plan p
WHERE p.code = us.plan_type;

ALTER TABLE user_subscription ADD CONSTRAINT fk_subscription_pricing_plan FOREIGN KEY (pricing_plan_id) REFERENCES pricing_plan(id) ON DELETE RESTRICT;
ALTER TABLE user_subscription ALTER COLUMN pricing_plan_id SET NOT NULL;
ALTER TABLE user_subscription ALTER COLUMN plan_code_snapshot SET NOT NULL;
ALTER TABLE user_subscription ALTER COLUMN plan_name_snapshot SET NOT NULL;
ALTER TABLE user_subscription ALTER COLUMN price_minor_snapshot SET NOT NULL;
ALTER TABLE user_subscription ALTER COLUMN currency_snapshot SET NOT NULL;
ALTER TABLE user_subscription ALTER COLUMN features_snapshot SET NOT NULL;
ALTER TABLE user_subscription DROP COLUMN plan_type;
CREATE INDEX idx_subscription_pricing_plan ON user_subscription (pricing_plan_id);
