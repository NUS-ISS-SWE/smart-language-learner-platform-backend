--liquibase formatted sql

--changeset yasin:011-orchestrator-durable-worker
ALTER TABLE agent_workflow_run
    ADD COLUMN IF NOT EXISTS claimed_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS lease_expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS next_run_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS lock_version INTEGER NOT NULL DEFAULT 0;

UPDATE agent_workflow_run
SET next_run_at = COALESCE(next_run_at, updated_at, created_at, CURRENT_TIMESTAMP)
WHERE next_run_at IS NULL;

ALTER TABLE agent_workflow_run
    ALTER COLUMN next_run_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_agent_workflow_claimable
    ON agent_workflow_run (next_run_at, created_at)
    WHERE status NOT IN ('COMPLETED', 'FAILED');

CREATE TABLE IF NOT EXISTS agent_workflow_outbox (
    event_id         UUID PRIMARY KEY,
    workflow_id      UUID NOT NULL REFERENCES agent_workflow_run(workflow_id),
    event_type       VARCHAR(32) NOT NULL,
    dedupe_key       VARCHAR(255) NOT NULL UNIQUE,
    topic            VARCHAR(255) NOT NULL,
    message_key      VARCHAR(255) NOT NULL,
    payload          JSONB NOT NULL,
    attempt_count    INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    claimed_by       VARCHAR(255),
    lease_expires_at TIMESTAMPTZ,
    next_attempt_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lock_version     INTEGER NOT NULL DEFAULT 0,
    last_error       JSONB,
    published_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_agent_workflow_outbox_claimable
    ON agent_workflow_outbox (next_attempt_at, created_at)
    WHERE published_at IS NULL;

-- Preserve any completion produced by the earlier direct-publish worker during
-- an in-place upgrade. The consumer remains idempotent by request ID.
INSERT INTO agent_workflow_outbox (
    event_id, workflow_id, event_type, dedupe_key, topic, message_key, payload,
    next_attempt_at, created_at
)
SELECT
    md5(workflow_id::text || ':completion')::UUID,
    workflow_id,
    'COMPLETION',
    workflow_id::text || ':completion',
    'SLLP_COMPOSITION_GRADING_COMPLETED_TOPIC',
    composition_id::text,
    completion_event,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM agent_workflow_run
WHERE completion_event IS NOT NULL
  AND completion_published_at IS NULL
ON CONFLICT (dedupe_key) DO NOTHING;
