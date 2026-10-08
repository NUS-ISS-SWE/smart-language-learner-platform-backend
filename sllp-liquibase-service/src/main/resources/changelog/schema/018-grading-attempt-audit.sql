--liquibase formatted sql

--changeset codex:018-grading-attempt-audit
CREATE TABLE grading_attempt (
    id UUID PRIMARY KEY,
    request_id VARCHAR(255) NOT NULL,
    context_id VARCHAR(255) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    attempt_number INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    composition_id BIGINT,
    user_id BIGINT NOT NULL,
    task_id VARCHAR(36) NOT NULL,
    artifact_id VARCHAR(36) NOT NULL,
    received_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    model_version VARCHAR(255),
    prompt_version VARCHAR(255),
    prompt_tokens BIGINT,
    completion_tokens BIGINT,
    cached_input_tokens BIGINT,
    total_tokens BIGINT,
    estimated_cost_micros BIGINT,
    result_payload TEXT,
    error_code VARCHAR(100),
    error_message VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_grading_attempt_request_attempt UNIQUE (request_id, attempt_number),
    CONSTRAINT ck_grading_attempt_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_grading_attempt_request_id ON grading_attempt (request_id);
CREATE INDEX idx_grading_attempt_status_completed ON grading_attempt (status, completed_at);
GRANT SELECT, INSERT, UPDATE, DELETE ON grading_attempt TO sllp_runtime;
