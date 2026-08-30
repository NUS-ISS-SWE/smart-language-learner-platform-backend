--liquibase formatted sql
--changeset yasin:composition-grading-progress

ALTER TABLE composition
    ADD COLUMN IF NOT EXISTS grading_progress_message VARCHAR(255);
