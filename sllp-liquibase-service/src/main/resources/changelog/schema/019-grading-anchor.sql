--liquibase formatted sql

--changeset codex:019-grading-anchor
CREATE TABLE grading_anchor (
    id BIGSERIAL PRIMARY KEY,
    language VARCHAR(20) NOT NULL,
    difficulty_level VARCHAR(30) NOT NULL,
    title VARCHAR(255),
    composition TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_grading_anchor_language
        CHECK (language IN ('ENGLISH', 'CHINESE')),
    CONSTRAINT ck_grading_anchor_difficulty_level
        CHECK (difficulty_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'))
);

CREATE TABLE grading_anchor_score (
    id BIGSERIAL PRIMARY KEY,
    anchor_id BIGINT NOT NULL,
    criterion_name VARCHAR(200) NOT NULL,
    score INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_grading_anchor_score_anchor
        FOREIGN KEY (anchor_id) REFERENCES grading_anchor(id) ON DELETE CASCADE,
    CONSTRAINT ck_grading_anchor_score_range
        CHECK (score BETWEEN 1 AND 5)
);

GRANT SELECT, INSERT, UPDATE, DELETE ON grading_anchor, grading_anchor_score TO sllp_runtime;
GRANT USAGE, SELECT, UPDATE ON SEQUENCE grading_anchor_id_seq, grading_anchor_score_id_seq TO sllp_runtime;
