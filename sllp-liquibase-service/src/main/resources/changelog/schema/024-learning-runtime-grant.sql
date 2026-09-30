--liquibase formatted sql

--changeset codex:024-learning-runtime-grant
-- Runtime grants are evaluated when this migration runs. The learning tables
-- were created after the original runtime-role grant, so grant only the
-- privileges that the learning service requires on its own persistence model.
GRANT SELECT, INSERT, UPDATE, DELETE ON
    challenge_template,
    challenge_submission,
    learning_goal,
    achievement_definition,
    achievement_award,
    assessment_learning_event,
    learning_activity_day
TO sllp_runtime;

GRANT USAGE, SELECT, UPDATE ON
    challenge_template_id_seq,
    challenge_submission_id_seq,
    learning_goal_id_seq,
    achievement_definition_id_seq,
    achievement_award_id_seq,
    learning_activity_day_id_seq
TO sllp_runtime;
