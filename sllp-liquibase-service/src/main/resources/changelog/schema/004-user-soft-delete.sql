--liquibase formatted sql

--changeset yasin:004-app-user-deleted-at
-- Add soft-delete support to app_user table
ALTER TABLE app_user ADD COLUMN deleted_at TIMESTAMP DEFAULT NULL;
CREATE INDEX idx_app_user_deleted_at ON app_user (deleted_at);
