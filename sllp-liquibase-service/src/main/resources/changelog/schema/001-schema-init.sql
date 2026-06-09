--liquibase formatted sql

--changeset mingwei:init-setup-user-table
CREATE TABLE IF NOT EXISTS app_user (
    id                            BIGSERIAL PRIMARY KEY,
    username                      VARCHAR(255) NOT NULL UNIQUE,
    email                         VARCHAR(255) NOT NULL UNIQUE,
    password                      TEXT NOT NULL,
    status                        VARCHAR(50) DEFAULT 'INACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING_EMAIL_VERIFICATION')),
    preferred_language            VARCHAR(50) DEFAULT 'ENGLISH' CHECK (preferred_language IN ('ENGLISH', 'CHINESE')),
    difficulty_level              VARCHAR(50) DEFAULT 'BEGINNER' CHECK (difficulty_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    created_at                    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    role                          VARCHAR(50) DEFAULT 'USER_FREE_TRIAL' CHECK (role IN ('ADMIN', 'USER_FREE_TRIAL', 'USER_BASIC', 'USER_PREMIUM')),
    token                         BIGINT,
    total_token_consumption       BIGINT,
    subscription_start_date       DATE,
    subscription_end_date         DATE,
    subscription_duration_in_days INTEGER
);

--changeset mingwei:init-setup-composition-table
CREATE TABLE IF NOT EXISTS composition (
    id                 BIGSERIAL PRIMARY KEY,
    username           VARCHAR(255) NOT NULL,
    title              VARCHAR(255),
    content            TEXT,
    composition_source VARCHAR(50) NOT NULL DEFAULT 'RAW_TEXT' CHECK (composition_source IN ('RAW_TEXT', 'FILE_PDF', 'FILE_DOCX', 'SYSTEM')),
    language           VARCHAR(50) NOT NULL DEFAULT 'ENGLISH' CHECK (language IN ('ENGLISH', 'CHINESE')),
    composition_status VARCHAR(50) NOT NULL DEFAULT 'DRAFT' CHECK (composition_status IN ('DRAFT', 'GENERATING', 'GENERATION_FAILED', 'REVIEWING', 'REVIEWED')),
    system_feedback    TEXT,
    storage_path       VARCHAR(255),
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
