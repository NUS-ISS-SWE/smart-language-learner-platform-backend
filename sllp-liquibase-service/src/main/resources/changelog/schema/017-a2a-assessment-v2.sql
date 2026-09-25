--liquibase formatted sql
--changeset yasin:017-a2a-assessment-v2

ALTER TABLE agent_workflow_run
    ADD COLUMN IF NOT EXISTS evidence_output JSONB;

ALTER TABLE agent_workflow_run
    DROP CONSTRAINT IF EXISTS agent_workflow_run_status_check;

ALTER TABLE agent_workflow_run
    ADD CONSTRAINT agent_workflow_run_status_check
    CHECK (status IN ('RECEIVED', 'EVIDENCING', 'GRADING', 'REVIEWING', 'REGRADING', 'SUGGESTING', 'COMPLETED', 'FAILED'));
