--liquibase formatted sql

--changeset codex:016-composition-source-document
-- A source document lives in document-service, so retain its opaque identifier
-- here rather than creating a cross-service database foreign key.
ALTER TABLE composition
    ADD COLUMN IF NOT EXISTS source_document_id VARCHAR(36);
