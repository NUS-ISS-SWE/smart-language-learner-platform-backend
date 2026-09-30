--liquibase formatted sql

--changeset codex:020-learning-challenge-schema
CREATE TABLE IF NOT EXISTS challenge_template (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    cadence VARCHAR(10) NOT NULL CHECK (cadence IN ('DAILY', 'WEEKLY')),
    language VARCHAR(20) NOT NULL CHECK (language IN ('ENGLISH', 'CHINESE')),
    title VARCHAR(255) NOT NULL,
    prompt TEXT NOT NULL,
    min_response_units INTEGER NOT NULL CHECK (min_response_units > 0),
    max_response_units INTEGER NOT NULL CHECK (max_response_units >= min_response_units),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0 CHECK (display_order >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_challenge_template_rotation
    ON challenge_template (cadence, language, active, display_order, id);

CREATE TABLE IF NOT EXISTS challenge_submission (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    challenge_template_id BIGINT NOT NULL REFERENCES challenge_template(id),
    cadence VARCHAR(10) NOT NULL CHECK (cadence IN ('DAILY', 'WEEKLY')),
    cycle_key VARCHAR(32) NOT NULL,
    activity_date DATE NOT NULL,
    content TEXT NOT NULL,
    title_snapshot VARCHAR(255) NOT NULL,
    prompt_snapshot TEXT NOT NULL,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_challenge_submission_cycle UNIQUE (user_id, cadence, cycle_key)
);
CREATE INDEX IF NOT EXISTS idx_challenge_submission_user_activity
    ON challenge_submission (user_id, activity_date DESC);
CREATE INDEX IF NOT EXISTS idx_challenge_submission_user_cadence_submitted
    ON challenge_submission (user_id, cadence, submitted_at DESC);

CREATE TABLE IF NOT EXISTS learning_goal (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    metric VARCHAR(48) NOT NULL CHECK (metric IN ('DAILY_CHALLENGE_COMPLETIONS', 'WEEKLY_CHALLENGE_COMPLETIONS', 'REVIEWED_COMPOSITIONS', 'OVERALL_ASSESSMENT_SCORE', 'VOCABULARY_RUBRIC_SCORE')),
    target NUMERIC(7,2) NOT NULL CHECK (target > 0),
    progress NUMERIC(7,2) NOT NULL DEFAULT 0 CHECK (progress >= 0),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED', 'EXPIRED')),
    starts_on DATE NOT NULL,
    due_on DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CHECK (due_on IS NULL OR due_on >= starts_on)
);
CREATE INDEX IF NOT EXISTS idx_learning_goal_user_status ON learning_goal (user_id, status, created_at DESC);

CREATE TABLE IF NOT EXISTS achievement_definition (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(500) NOT NULL,
    category VARCHAR(48) NOT NULL,
    threshold INTEGER NOT NULL CHECK (threshold > 0),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS achievement_award (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    achievement_definition_id BIGINT NOT NULL REFERENCES achievement_definition(id),
    awarded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    seen_at TIMESTAMP,
    CONSTRAINT uq_achievement_award_user_definition UNIQUE (user_id, achievement_definition_id)
);
CREATE INDEX IF NOT EXISTS idx_achievement_award_user_awarded ON achievement_award (user_id, awarded_at DESC);

CREATE TABLE IF NOT EXISTS assessment_learning_event (
    request_id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    composition_id BIGINT NOT NULL REFERENCES composition(id) ON DELETE CASCADE,
    score DECIMAL(5,2),
    completed_at TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_assessment_learning_event_user_completed
    ON assessment_learning_event (user_id, completed_at DESC);

CREATE TABLE IF NOT EXISTS learning_activity_day (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    activity_date DATE NOT NULL,
    CONSTRAINT uq_learning_activity_day UNIQUE (user_id, activity_date)
);
CREATE INDEX IF NOT EXISTS idx_learning_activity_day_user_date ON learning_activity_day (user_id, activity_date DESC);

--changeset codex:020-seed-learning-achievements
INSERT INTO achievement_definition (code, title, description, category, threshold) VALUES
    ('FIRST_CHALLENGE', 'First step', 'Complete your first writing challenge.', 'CHALLENGE_COMPLETIONS', 1),
    ('CHALLENGE_5', 'Finding your rhythm', 'Complete five writing challenges.', 'CHALLENGE_COMPLETIONS', 5),
    ('CHALLENGE_25', 'Writing habit', 'Complete twenty-five writing challenges.', 'CHALLENGE_COMPLETIONS', 25),
    ('STREAK_3', 'Three-day streak', 'Show up for three consecutive days.', 'STREAK_DAYS', 3),
    ('STREAK_7', 'Week of words', 'Show up for seven consecutive days.', 'STREAK_DAYS', 7),
    ('STREAK_30', 'Steady writer', 'Show up for thirty consecutive days.', 'STREAK_DAYS', 30),
    ('FIRST_WEEKLY_CHALLENGE', 'Weekly finisher', 'Complete your first weekly writing challenge.', 'WEEKLY_CHALLENGES', 1)
    ,('FIRST_REVIEWED_COMPOSITION', 'First reviewed composition', 'Receive feedback on your first full composition.', 'REVIEWED_COMPOSITIONS', 1)
    ,('REVIEWED_COMPOSITIONS_5', 'Five reviewed compositions', 'Receive feedback on five full compositions.', 'REVIEWED_COMPOSITIONS', 5)
    ,('OVERALL_SCORE_80', 'Writing breakthrough', 'Reach an overall assessment score of 80.', 'OVERALL_SCORE', 80)
    ,('VOCABULARY_SCORE_75', 'Word choice breakthrough', 'Reach a vocabulary and word-choice rubric score of 75.', 'VOCABULARY_SCORE', 75)
ON CONFLICT (code) DO NOTHING;

--changeset codex:020-seed-learning-challenges
INSERT INTO challenge_template (code, cadence, language, title, prompt, min_response_units, max_response_units, display_order) VALUES
    ('DAILY_EN_SMALL_WIN', 'DAILY', 'ENGLISH', 'A small win', 'Write about one small thing that went well today. Explain why it mattered to you.', 35, 80, 1),
    ('DAILY_EN_SNAPSHOT', 'DAILY', 'ENGLISH', 'Describe what you can see', 'Choose something near you. Describe it with at least two sensory details.', 35, 80, 2),
    ('DAILY_EN_JUST_AS', 'DAILY', 'ENGLISH', 'Just as I…', 'Continue this opening: “Just as I was about to leave, …” Include a clear change or surprise.', 35, 80, 3),
    ('DAILY_EN_HELPFUL_NOTE', 'DAILY', 'ENGLISH', 'A helpful note', 'Write a short note giving a friend useful advice for an upcoming task.', 35, 80, 4),
    ('DAILY_EN_KIND_REPLY', 'DAILY', 'ENGLISH', 'A kind reply', 'A classmate has had a difficult day. Write a supportive reply that feels personal and specific.', 35, 80, 5),
    ('DAILY_EN_CHANGE', 'DAILY', 'ENGLISH', 'A surprising change', 'Describe a familiar place that suddenly changes. Show how someone reacts.', 35, 80, 6),
    ('DAILY_EN_LEARNED', 'DAILY', 'ENGLISH', 'One thing I learned', 'Explain one thing you learned recently and one way you could use it.', 35, 80, 7),
    ('WEEKLY_EN_PLACE', 'WEEKLY', 'ENGLISH', 'A place worth revisiting', 'Describe a place you would like to visit again. Give two reasons and finish with a clear conclusion.', 120, 180, 1),
    ('WEEKLY_EN_PROBLEM', 'WEEKLY', 'ENGLISH', 'How I solved a small problem', 'Tell a short story about a problem, the steps you took, and what you learned from it.', 120, 180, 2),
    ('WEEKLY_EN_TIP', 'WEEKLY', 'ENGLISH', 'My best learning tip', 'Share one learning tip. Explain the steps and include an example of when it helps.', 120, 180, 3),
    ('WEEKLY_EN_FUTURE', 'WEEKLY', 'ENGLISH', 'A letter to my future self', 'Write a letter to your future self about a goal, a current challenge, and encouragement to keep going.', 120, 180, 4),
    ('DAILY_ZH_SMALL_WIN', 'DAILY', 'CHINESE', '一个小收获', '写一件今天顺利的小事，并说明它为什么对你重要。', 60, 160, 1),
    ('DAILY_ZH_SNAPSHOT', 'DAILY', 'CHINESE', '眼前的画面', '选择身边的一样事物，用至少两种感官细节来描述它。', 60, 160, 2),
    ('DAILY_ZH_JUST_AS', 'DAILY', 'CHINESE', '正当我……', '续写开头：“正当我准备离开时，……” 写出一个清楚的变化或惊喜。', 60, 160, 3),
    ('DAILY_ZH_HELPFUL_NOTE', 'DAILY', 'CHINESE', '一张有用的便条', '给朋友写一张短便条，为他即将完成的任务提供实用建议。', 60, 160, 4),
    ('DAILY_ZH_KIND_REPLY', 'DAILY', 'CHINESE', '温暖的回复', '一位同学今天心情不好。写一段具体而真诚的支持回复。', 60, 160, 5),
    ('DAILY_ZH_CHANGE', 'DAILY', 'CHINESE', '意外的变化', '描述一个熟悉的地方突然发生变化，并写出人物的反应。', 60, 160, 6),
    ('DAILY_ZH_LEARNED', 'DAILY', 'CHINESE', '我学到的一件事', '说明你最近学到的一件事，以及你可以怎样使用它。', 60, 160, 7),
    ('WEEKLY_ZH_PLACE', 'WEEKLY', 'CHINESE', '值得再去的地方', '描述一个你想再去的地方，给出两个理由，并用清楚的结尾收束全文。', 180, 320, 1),
    ('WEEKLY_ZH_PROBLEM', 'WEEKLY', 'CHINESE', '我怎样解决一个小问题', '写一个关于问题、解决步骤和你从中学到什么的短故事。', 180, 320, 2),
    ('WEEKLY_ZH_TIP', 'WEEKLY', 'CHINESE', '我最好的学习方法', '分享一个学习方法，说明步骤，并举例说明它在什么时候有用。', 180, 320, 3),
    ('WEEKLY_ZH_FUTURE', 'WEEKLY', 'CHINESE', '写给未来的自己', '给未来的自己写一封信，谈谈目标、目前的挑战，以及继续努力的鼓励。', 180, 320, 4)
ON CONFLICT (code) DO NOTHING;
