--liquibase formatted sql

--changeset codex:029-lakera-guard-detection
CREATE TABLE lakera_guard_detection (
    id UUID PRIMARY KEY,
    request_id VARCHAR(255) NOT NULL,
    context_id VARCHAR(255) NOT NULL,
    composition_id BIGINT,
    user_id BIGINT NOT NULL,
    screened_text TEXT NOT NULL,
    provider_request_uuid VARCHAR(255),
    action VARCHAR(32),
    provider_flagged BOOLEAN NOT NULL,
    detector_breakdown TEXT NOT NULL,
    provider_response TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_lakera_guard_detection_request
    ON lakera_guard_detection (request_id);
CREATE INDEX idx_lakera_guard_detection_composition
    ON lakera_guard_detection (composition_id);
CREATE INDEX idx_lakera_guard_detection_created
    ON lakera_guard_detection (created_at);

GRANT SELECT, INSERT ON lakera_guard_detection TO sllp_runtime;
