--liquibase formatted sql
--changeset yasin:agent-workflow-state

CREATE TABLE IF NOT EXISTS agent_workflow_run (
    workflow_id              UUID PRIMARY KEY,
    source_event_id          VARCHAR(255) NOT NULL UNIQUE,
    correlation_id           VARCHAR(255),
    composition_id           BIGINT NOT NULL,
    user_id                  BIGINT NOT NULL,
    status                   VARCHAR(32) NOT NULL CHECK (status IN ('RECEIVED', 'GRADING', 'REVIEWING', 'REGRADING', 'SUGGESTING', 'COMPLETED', 'FAILED')),
    input_payload            JSONB NOT NULL,
    grading_output           JSONB,
    review_output            JSONB,
    suggestion_output        JSONB,
    final_report             JSONB,
    completion_event         JSONB,
    completion_published_at  TIMESTAMPTZ,
    error_details            JSONB,
    grading_passes           INTEGER NOT NULL DEFAULT 0 CHECK (grading_passes >= 0),
    created_at               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at             TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_agent_workflow_composition_id ON agent_workflow_run (composition_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_agent_workflow_recovery ON agent_workflow_run (status) WHERE completion_published_at IS NULL;

CREATE TABLE IF NOT EXISTS agent_workflow_step_log (
    id          BIGSERIAL PRIMARY KEY,
    workflow_id UUID NOT NULL REFERENCES agent_workflow_run(workflow_id) ON DELETE CASCADE,
    step        VARCHAR(64) NOT NULL,
    status      VARCHAR(32) NOT NULL,
    attempt     INTEGER NOT NULL DEFAULT 0 CHECK (attempt >= 0),
    latency_ms  INTEGER,
    details     JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_agent_workflow_step_log_workflow ON agent_workflow_step_log (workflow_id, created_at);

