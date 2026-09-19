--liquibase formatted sql

--changeset codex:015-audit-runtime-role splitStatements:false
-- Application services do not run as the database owner. They can write normal
-- application data but may only insert/read audit evidence; the migrator remains
-- the sole owner for schema maintenance and disaster recovery.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'sllp_runtime') THEN
        CREATE ROLE sllp_runtime LOGIN PASSWORD '${audit_runtime_password}';
    ELSE
        ALTER ROLE sllp_runtime LOGIN PASSWORD '${audit_runtime_password}';
    END IF;
END $$;

GRANT CONNECT ON DATABASE sllp_main_db TO sllp_runtime;
GRANT USAGE ON SCHEMA public TO sllp_runtime;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO sllp_runtime;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO sllp_runtime;
REVOKE UPDATE, DELETE ON audit_event FROM sllp_runtime;
GRANT SELECT, INSERT ON audit_event TO sllp_runtime;
