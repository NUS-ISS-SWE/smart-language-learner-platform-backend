--liquibase formatted sql

--changeset codex:028-dev-admin-advanced-difficulty
-- Keep the development administrator on the advanced English rubric without
-- mutating the historical seed changeset that existing databases recorded.
UPDATE app_user
SET difficulty_level = 'ADVANCED'
WHERE username = 'admin'
  AND email = 'admin@sllp.dev'
  AND role = 'ADMIN';
