--liquibase formatted sql

--changeset codex:022-agent-review-queue
CREATE TABLE agent_review_queue (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL UNIQUE,
    composition_id BIGINT NOT NULL REFERENCES composition(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'RESOLVED')),
    failure_reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_agent_review_queue_open ON agent_review_queue (status, created_at DESC);
