--liquibase formatted sql

--changeset codex:018-composition-moderation splitStatements:false
ALTER TABLE composition
    ADD COLUMN IF NOT EXISTS moderation_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS moderation_reason_category VARCHAR(32),
    ADD COLUMN IF NOT EXISTS moderation_reason_note VARCHAR(500),
    ADD COLUMN IF NOT EXISTS moderated_by_id BIGINT,
    ADD COLUMN IF NOT EXISTS moderated_by_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS moderated_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE composition DROP CONSTRAINT IF EXISTS composition_moderation_status_check;
ALTER TABLE composition ADD CONSTRAINT composition_moderation_status_check
    CHECK (moderation_status IN ('ACTIVE', 'REMOVED'));
ALTER TABLE composition DROP CONSTRAINT IF EXISTS composition_moderation_reason_category_check;
ALTER TABLE composition ADD CONSTRAINT composition_moderation_reason_category_check
    CHECK (moderation_reason_category IS NULL OR moderation_reason_category IN
        ('SPAM_OR_IRRELEVANT', 'ABUSIVE_OR_HARMFUL', 'PRIVACY_OR_PERSONAL_DATA', 'COPYRIGHT_OR_IP', 'OTHER'));
CREATE INDEX IF NOT EXISTS idx_composition_moderation_created
    ON composition (moderation_status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_composition_moderation_user
    ON composition (user_id, moderation_status, created_at DESC);

CREATE TABLE IF NOT EXISTS composition_moderation_decision (
    id UUID PRIMARY KEY,
    composition_id BIGINT NOT NULL REFERENCES composition(id) ON DELETE CASCADE,
    action VARCHAR(16) NOT NULL CHECK (action IN ('REMOVE', 'RESTORE')),
    reason_category VARCHAR(32),
    reason_note VARCHAR(500),
    moderator_id BIGINT NOT NULL REFERENCES app_user(id),
    moderator_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_composition_moderation_decision_composition
    ON composition_moderation_decision (composition_id, created_at DESC);

CREATE OR REPLACE FUNCTION reject_composition_moderation_decision_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'composition moderation decisions are append-only';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS composition_moderation_decision_immutable ON composition_moderation_decision;
CREATE TRIGGER composition_moderation_decision_immutable
BEFORE UPDATE OR DELETE ON composition_moderation_decision
FOR EACH ROW EXECUTE FUNCTION reject_composition_moderation_decision_mutation();
