--liquibase formatted sql

--changeset codex:027-learning-focused-goals
ALTER TABLE learning_goal ADD COLUMN IF NOT EXISTS focused BOOLEAN NOT NULL DEFAULT FALSE;
WITH newest_active_goal AS (SELECT DISTINCT ON (user_id) id FROM learning_goal WHERE status = 'ACTIVE' ORDER BY user_id, created_at DESC)
UPDATE learning_goal SET focused = TRUE WHERE id IN (SELECT id FROM newest_active_goal);
CREATE UNIQUE INDEX IF NOT EXISTS uq_learning_goal_one_focused_per_user ON learning_goal (user_id) WHERE focused = TRUE;
