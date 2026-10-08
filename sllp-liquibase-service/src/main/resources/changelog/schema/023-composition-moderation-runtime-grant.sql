--liquibase formatted sql

--changeset codex:023-composition-moderation-runtime-grant
-- The moderation table was introduced after the runtime-role grant migration.
-- Moderation decisions are append-only, so application services need only to
-- read prior decisions and insert new ones.
GRANT SELECT, INSERT ON composition_moderation_decision TO sllp_runtime;
