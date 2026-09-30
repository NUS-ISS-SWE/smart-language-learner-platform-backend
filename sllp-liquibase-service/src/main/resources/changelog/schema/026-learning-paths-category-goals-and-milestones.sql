--liquibase formatted sql

--changeset codex:026-learning-paths-category-goals
ALTER TABLE challenge_template ADD COLUMN IF NOT EXISTS category VARCHAR(32) NOT NULL DEFAULT 'REFLECTION';
ALTER TABLE challenge_template ADD COLUMN IF NOT EXISTS theme_path VARCHAR(64) NOT NULL DEFAULT 'EVERYDAY_CONFIDENCE';
ALTER TABLE challenge_submission ADD COLUMN IF NOT EXISTS category_snapshot VARCHAR(32) NOT NULL DEFAULT 'REFLECTION';
ALTER TABLE learning_goal ADD COLUMN IF NOT EXISTS category VARCHAR(32);

UPDATE challenge_template
SET category = CASE
    WHEN code LIKE '%SNAPSHOT%' OR code LIKE '%PLACE%' THEN 'DESCRIPTION'
    WHEN code LIKE '%JUST_AS%' OR code LIKE '%CHANGE%' OR code LIKE '%PROBLEM%' THEN 'STORYTELLING'
    WHEN code LIKE '%NOTE%' OR code LIKE '%TIP%' OR code LIKE '%KIND_REPLY%' THEN 'PRACTICAL'
    ELSE 'REFLECTION'
END,
theme_path = CASE
    WHEN code LIKE '%SNAPSHOT%' OR code LIKE '%PLACE%' THEN 'VIVID_DESCRIPTION'
    WHEN code LIKE '%JUST_AS%' OR code LIKE '%CHANGE%' OR code LIKE '%PROBLEM%' THEN 'STORY_BUILDER'
    WHEN code LIKE '%NOTE%' OR code LIKE '%TIP%' OR code LIKE '%KIND_REPLY%' THEN 'HELPFUL_COMMUNICATION'
    ELSE 'EVERYDAY_CONFIDENCE'
END;

UPDATE challenge_submission submission
SET category_snapshot = template.category
FROM challenge_template template
WHERE submission.challenge_template_id = template.id;

ALTER TABLE learning_goal DROP CONSTRAINT IF EXISTS learning_goal_metric_check;
ALTER TABLE learning_goal ADD CONSTRAINT learning_goal_metric_check CHECK (metric IN (
    'DAILY_CHALLENGE_COMPLETIONS', 'WEEKLY_CHALLENGE_COMPLETIONS', 'CATEGORY_CHALLENGE_COMPLETIONS',
    'REVIEWED_COMPOSITIONS', 'OVERALL_ASSESSMENT_SCORE', 'VOCABULARY_RUBRIC_SCORE', 'CURRENT_STREAK_DAYS'
));
CREATE INDEX IF NOT EXISTS idx_challenge_submission_user_category_submitted
    ON challenge_submission (user_id, category_snapshot, submitted_at DESC);

--changeset codex:026-seed-personal-best-achievements
INSERT INTO achievement_definition (code, title, description, category, threshold) VALUES
    ('PERSONAL_BEST_60', 'New personal best', 'Set a new personal-best overall assessment score of 60 or higher.', 'PERSONAL_BEST_SCORE', 60),
    ('PERSONAL_BEST_80', 'Personal best: 80', 'Set a new personal-best overall assessment score of 80 or higher.', 'PERSONAL_BEST_SCORE', 80),
    ('PERSONAL_BEST_90', 'Personal best: 90', 'Set a new personal-best overall assessment score of 90 or higher.', 'PERSONAL_BEST_SCORE', 90)
ON CONFLICT (code) DO NOTHING;
