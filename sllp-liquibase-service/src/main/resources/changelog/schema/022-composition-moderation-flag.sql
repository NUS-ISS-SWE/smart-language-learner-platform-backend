--liquibase formatted sql

--changeset codex:022-composition-moderation-flag
CREATE TABLE composition_moderation_flag (
    id UUID PRIMARY KEY,
    request_id VARCHAR(255) NOT NULL,
    context_id VARCHAR(255) NOT NULL,
    composition_id BIGINT,
    user_id BIGINT NOT NULL,
    moderated_text TEXT NOT NULL,
    provider_response_id VARCHAR(255),
    model VARCHAR(255) NOT NULL,
    provider_flagged BOOLEAN NOT NULL,
    matched_category_scores TEXT NOT NULL,
    threshold DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_composition_moderation_threshold CHECK (threshold >= 0 AND threshold <= 1)
);

CREATE INDEX idx_composition_moderation_flag_composition
    ON composition_moderation_flag (composition_id);
CREATE INDEX idx_composition_moderation_flag_created
    ON composition_moderation_flag (created_at);

GRANT SELECT, INSERT, UPDATE, DELETE ON composition_moderation_flag TO sllp_runtime;
