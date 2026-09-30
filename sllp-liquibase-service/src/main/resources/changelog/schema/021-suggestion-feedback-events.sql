--liquibase formatted sql

--changeset codex:021-suggestion-feedback-events
CREATE TABLE suggestion_feedback (
    id BIGSERIAL PRIMARY KEY,
    composition_id BIGINT NOT NULL REFERENCES composition(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    suggestion_index INTEGER NOT NULL CHECK (suggestion_index >= 0),
    rating VARCHAR(20) NOT NULL CHECK (rating IN ('HELPFUL', 'NOT_HELPFUL')),
    reason VARCHAR(20) CHECK (reason IN ('NOT_RELEVANT', 'UNCLEAR', 'TOO_GENERIC', 'UNSAFE')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_suggestion_feedback UNIQUE (composition_id, user_id, suggestion_index)
);
CREATE INDEX idx_suggestion_feedback_rating ON suggestion_feedback (rating, created_at DESC);
