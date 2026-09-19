--liquibase formatted sql

--changeset yasin:010-usage-reporting-query-indexes
-- These composite indexes match the bounded per-user and per-agent reporting predicates.
-- Validate them with EXPLAIN (ANALYZE, BUFFERS) against production-like volumes before adding more.
CREATE INDEX IF NOT EXISTS idx_composition_user_created_at ON composition (user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_documents_user_created_at ON documents (user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_evaluation_requested_at ON evaluation (requested_at);
CREATE INDEX IF NOT EXISTS idx_agent_workflow_user_created_at ON agent_workflow_run (user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_agent_workflow_step_log_step_created_at ON agent_workflow_step_log (step, created_at);
