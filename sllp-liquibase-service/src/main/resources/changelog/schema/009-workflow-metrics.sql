--liquibase formatted sql

--changeset yasin:009-normalize-agent-usage-events
-- One assessment workflow can call several agents, so request_id cannot be the ledger's unique key.
ALTER TABLE ai_usage_event ADD COLUMN IF NOT EXISTS event_id UUID;
ALTER TABLE ai_usage_event ADD COLUMN IF NOT EXISTS workflow_id UUID;
ALTER TABLE ai_usage_event ADD COLUMN IF NOT EXISTS agent VARCHAR(50);
UPDATE ai_usage_event
SET event_id = request_id,
    agent = COALESCE(agent, 'grading')
WHERE event_id IS NULL OR agent IS NULL;
ALTER TABLE ai_usage_event DROP CONSTRAINT IF EXISTS ai_usage_event_request_id_key;
DROP INDEX IF EXISTS ai_usage_event_request_id_key;
ALTER TABLE ai_usage_event ALTER COLUMN request_id DROP NOT NULL;
ALTER TABLE ai_usage_event ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE ai_usage_event ALTER COLUMN agent SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_ai_usage_event_event_id ON ai_usage_event (event_id);
CREATE INDEX IF NOT EXISTS idx_ai_usage_event_agent_created_at ON ai_usage_event (agent, created_at);
CREATE INDEX IF NOT EXISTS idx_ai_usage_event_workflow_id ON ai_usage_event (workflow_id);

--changeset yasin:009-workflow-metrics-indexes
CREATE INDEX IF NOT EXISTS idx_agent_workflow_created_at ON agent_workflow_run (created_at);
CREATE INDEX IF NOT EXISTS idx_agent_workflow_completed_at ON agent_workflow_run (completed_at);
CREATE INDEX IF NOT EXISTS idx_agent_workflow_step_log_created_at ON agent_workflow_step_log (created_at);
