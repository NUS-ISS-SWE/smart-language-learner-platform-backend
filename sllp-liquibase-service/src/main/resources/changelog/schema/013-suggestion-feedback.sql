--liquibase formatted sql
--changeset yasin:typed-suggestion-feedback

ALTER TABLE composition ADD COLUMN IF NOT EXISTS suggestion_status VARCHAR(64);
ALTER TABLE composition ADD COLUMN IF NOT EXISTS suggestions TEXT;

ALTER TABLE evaluation ADD COLUMN IF NOT EXISTS suggestion_status VARCHAR(64);
ALTER TABLE evaluation ADD COLUMN IF NOT EXISTS suggestions TEXT;
