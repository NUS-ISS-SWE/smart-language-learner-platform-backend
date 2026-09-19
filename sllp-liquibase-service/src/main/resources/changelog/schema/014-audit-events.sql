--liquibase formatted sql

--changeset codex:014-audit-events splitStatements:false
CREATE TABLE audit_event (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    service VARCHAR(80) NOT NULL,
    category VARCHAR(48) NOT NULL,
    action VARCHAR(120) NOT NULL,
    outcome VARCHAR(24) NOT NULL CHECK (outcome IN ('SUCCESS', 'FAILURE', 'DENIED')),
    actor_type VARCHAR(32) NOT NULL,
    actor_id VARCHAR(255),
    actor_name VARCHAR(255),
    actor_role VARCHAR(64),
    target_type VARCHAR(80),
    target_id VARCHAR(255),
    request_id UUID,
    correlation_id VARCHAR(255),
    source_ip VARCHAR(64),
    user_agent VARCHAR(512),
    http_method VARCHAR(12),
    http_path VARCHAR(512),
    http_status INTEGER,
    before_data JSONB,
    after_data JSONB,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    error_code VARCHAR(100)
);

CREATE INDEX idx_audit_event_occurred_at ON audit_event (occurred_at DESC);
CREATE INDEX idx_audit_event_actor ON audit_event (actor_type, actor_id, occurred_at DESC);
CREATE INDEX idx_audit_event_target ON audit_event (target_type, target_id, occurred_at DESC);
CREATE INDEX idx_audit_event_action ON audit_event (action, occurred_at DESC);
CREATE INDEX idx_audit_event_outcome ON audit_event (outcome, occurred_at DESC);
CREATE INDEX idx_audit_event_request_id ON audit_event (request_id);
CREATE INDEX idx_audit_event_correlation_id ON audit_event (correlation_id);

-- Audit records are evidence, not editable application data. Database owners can still
-- perform recovery work, but ordinary application DML cannot modify or delete a record.
CREATE OR REPLACE FUNCTION reject_audit_event_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_event records are append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_event_immutable
BEFORE UPDATE OR DELETE ON audit_event
FOR EACH ROW EXECUTE FUNCTION reject_audit_event_mutation();
