--liquibase formatted sql

--changeset codex:025-agent-runtime-grants
-- Runtime grants in migration 015 cover only objects that existed at that time.
-- Grant access explicitly for the later agent-support tables and their identity sequences.
GRANT SELECT, INSERT, UPDATE, DELETE
ON TABLE assessment_quota_reservation, suggestion_feedback, agent_review_queue
TO sllp_runtime;

GRANT USAGE, SELECT, UPDATE
ON SEQUENCE assessment_quota_reservation_id_seq, suggestion_feedback_id_seq
TO sllp_runtime;
