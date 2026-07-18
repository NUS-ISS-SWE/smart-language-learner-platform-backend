--liquibase formatted sql

--changeset yasin:002-composition-user-id
-- Replace username VARCHAR with user_id BIGINT FK → app_user(id) on composition table
ALTER TABLE composition ADD COLUMN user_id BIGINT;
UPDATE composition c SET user_id = (SELECT id FROM app_user WHERE username = c.username);
ALTER TABLE composition ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE composition ADD CONSTRAINT fk_composition_user
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE;
ALTER TABLE composition DROP COLUMN username;
CREATE INDEX idx_composition_user_id ON composition (user_id);

--changeset yasin:002-drop-users-table
-- Remove conflicting duplicate 'users' table (was from inactive Liquibase XML changelog)
DROP TABLE IF EXISTS users CASCADE;

--changeset yasin:002-recreate-documents-bigint
-- Recreate documents with BIGINT PK and BIGINT user_id FK → app_user(id)
-- Safe to drop: dev environment only, no production data
DROP TABLE IF EXISTS documents CASCADE;
CREATE TABLE documents (
    id             BIGSERIAL    PRIMARY KEY,
    user_id        BIGINT        NOT NULL,
    filename       VARCHAR(255)  NOT NULL,
    s3_key         VARCHAR(500)  NOT NULL,
    file_type      VARCHAR(50)   NOT NULL,
    file_size      BIGINT        NOT NULL,
    status         VARCHAR(50)   NOT NULL DEFAULT 'UPLOADED'
                   CHECK (status IN ('UPLOADED', 'PROCESSING', 'READY', 'FAILED')),
    extracted_text TEXT,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_documents_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);
CREATE INDEX idx_documents_user_id ON documents (user_id);

--changeset yasin:002-create-user-subscription
-- Extract subscription data from app_user into a normalized table
CREATE TABLE user_subscription (
    id            BIGSERIAL    PRIMARY KEY,
    user_id       BIGINT        NOT NULL,
    plan_type     VARCHAR(50)   NOT NULL DEFAULT 'FREE_TRIAL'
                  CHECK (plan_type IN ('FREE_TRIAL', 'BASIC', 'PREMIUM')),
    start_date    DATE,
    end_date      DATE,
    duration_days INTEGER,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_subscription_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);
CREATE INDEX idx_subscription_user_id ON user_subscription (user_id);

--changeset yasin:002-migrate-subscription-data
INSERT INTO user_subscription (user_id, plan_type, start_date, end_date, duration_days)
SELECT
    id,
    CASE role
        WHEN 'USER_BASIC'   THEN 'BASIC'
        WHEN 'USER_PREMIUM' THEN 'PREMIUM'
        ELSE 'FREE_TRIAL'
    END,
    subscription_start_date,
    subscription_end_date,
    subscription_duration_in_days
FROM app_user
WHERE subscription_start_date IS NOT NULL
   OR subscription_end_date IS NOT NULL
   OR subscription_duration_in_days IS NOT NULL;

--changeset yasin:002-drop-subscription-cols
ALTER TABLE app_user DROP COLUMN IF EXISTS subscription_start_date;
ALTER TABLE app_user DROP COLUMN IF EXISTS subscription_end_date;
ALTER TABLE app_user DROP COLUMN IF EXISTS subscription_duration_in_days;

--changeset yasin:002-rename-token-fields splitStatements:false
-- Rename only if old column names still exist (idempotent: safe if already renamed by init script)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'app_user' AND column_name = 'token') THEN
        ALTER TABLE app_user RENAME COLUMN token TO ai_token_balance;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'app_user' AND column_name = 'total_token_consumption') THEN
        ALTER TABLE app_user RENAME COLUMN total_token_consumption TO ai_token_used;
    END IF;
END $$;

--changeset yasin:002-create-evaluation
-- Add persistent evaluation records so grading history is queryable
CREATE TABLE evaluation (
    id             BIGSERIAL    PRIMARY KEY,
    composition_id BIGINT        NOT NULL,
    score          DECIMAL(5, 2),
    feedback       TEXT,
    success        BOOLEAN       NOT NULL DEFAULT TRUE,
    failure_reason TEXT,
    evaluated_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_evaluation_composition
        FOREIGN KEY (composition_id) REFERENCES composition(id) ON DELETE CASCADE
);
CREATE INDEX idx_evaluation_composition_id ON evaluation (composition_id);
