--liquibase formatted sql

--changeset yasin:005-usage-metrics-indexes
-- These indexes keep date-range aggregates on the admin dashboard efficient.
CREATE INDEX IF NOT EXISTS idx_app_user_created_at_active ON app_user (created_at) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_composition_created_at ON composition (created_at);
CREATE INDEX IF NOT EXISTS idx_composition_reviewed_updated_at ON composition (updated_at) WHERE composition_status = 'REVIEWED';
CREATE INDEX IF NOT EXISTS idx_documents_created_at ON documents (created_at);
