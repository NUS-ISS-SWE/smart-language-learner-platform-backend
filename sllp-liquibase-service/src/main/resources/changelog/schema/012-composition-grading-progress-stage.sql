--liquibase formatted sql

--changeset yasin:012-composition-grading-progress-stage
ALTER TABLE composition
    ADD COLUMN IF NOT EXISTS grading_progress_stage VARCHAR(32);
