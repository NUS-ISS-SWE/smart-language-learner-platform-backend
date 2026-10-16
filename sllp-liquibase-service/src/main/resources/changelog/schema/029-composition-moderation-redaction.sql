--liquibase formatted sql

--changeset codex:029-composition-moderation-redaction
-- Preserve historical decisions and text lengths without retaining learner text.
ALTER TABLE composition_moderation_flag ADD COLUMN IF NOT EXISTS content_sha256 VARCHAR(64);
ALTER TABLE composition_moderation_flag ADD COLUMN IF NOT EXISTS content_length INTEGER;
ALTER TABLE composition_moderation_flag ADD COLUMN IF NOT EXISTS stage VARCHAR(32) NOT NULL DEFAULT 'COMPOSITION_INPUT';
ALTER TABLE composition_moderation_flag ADD COLUMN IF NOT EXISTS decision VARCHAR(32) NOT NULL DEFAULT 'BLOCKED';
UPDATE composition_moderation_flag
SET content_sha256 = repeat('0', 64),
    content_length = length(moderated_text)
WHERE content_sha256 IS NULL AND moderated_text IS NOT NULL;
ALTER TABLE composition_moderation_flag ALTER COLUMN content_sha256 SET NOT NULL;
ALTER TABLE composition_moderation_flag ALTER COLUMN content_length SET NOT NULL;
ALTER TABLE composition_moderation_flag DROP COLUMN moderated_text;
