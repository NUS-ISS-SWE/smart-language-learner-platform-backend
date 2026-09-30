--liquibase formatted sql

--changeset codex:025-learning-streak-goal
ALTER TABLE learning_goal DROP CONSTRAINT IF EXISTS learning_goal_metric_check;
ALTER TABLE learning_goal ADD CONSTRAINT learning_goal_metric_check CHECK (metric IN (
    'DAILY_CHALLENGE_COMPLETIONS', 'WEEKLY_CHALLENGE_COMPLETIONS', 'CURRENT_STREAK_DAYS',
    'REVIEWED_COMPOSITIONS', 'OVERALL_ASSESSMENT_SCORE', 'VOCABULARY_RUBRIC_SCORE'
));
