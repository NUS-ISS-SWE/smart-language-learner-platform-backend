--liquibase formatted sql
--changeset mingwei:init-rubric-criterion-table

CREATE TABLE IF NOT EXISTS rubric_criterion (
    id                    BIGSERIAL PRIMARY KEY,
    language              VARCHAR(20) NOT NULL CHECK (language IN ('ENGLISH', 'CHINESE')),
    difficulty_level      VARCHAR(30) NOT NULL CHECK (difficulty_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')),
    criterion_name        VARCHAR(200) NOT NULL,
    criterion_description TEXT NOT NULL,
    weight_percentage     DECIMAL(5,2) NOT NULL CHECK (weight_percentage > 0 AND weight_percentage <= 100),
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO rubric_criterion (
    language,
    difficulty_level,
    criterion_name,
    criterion_description,
    weight_percentage
)
VALUES
    (
        'ENGLISH',
        'BEGINNER',
        'TASK_IDEAS',
        'Assess whether the student responds to the topic with a simple, relevant and understandable sequence of ideas. For Primary 1–2, reward clarity and completeness rather than sophisticated development.',
        25.00
    ),
    (
        'ENGLISH',
        'BEGINNER',
        'ORGANISATION_COHESION',
        'Assess whether events and ideas are arranged in a basic logical order with a recognisable beginning, middle and ending. Simple connecting words are sufficient at this level.',
        20.00
    ),
    (
        'ENGLISH',
        'BEGINNER',
        'GRAMMAR_SENTENCE_CONTROL',
        'Assess whether the student can write understandable and mostly complete simple sentences. Consider whether grammar errors affect meaning rather than only counting errors.',
        20.00
    ),
    (
        'ENGLISH',
        'BEGINNER',
        'VOCABULARY_WORD_CHOICE',
        'Assess whether the student uses familiar and age-appropriate words accurately to communicate actions, people, places and feelings.',
        15.00
    ),
    (
        'ENGLISH',
        'BEGINNER',
        'MECHANICS',
        'Assess basic spelling, capitalisation and sentence-ending punctuation. Minor mistakes should not be heavily penalised when meaning remains clear.',
        10.00
    ),
    (
        'ENGLISH',
        'BEGINNER',
        'EXPRESSION_DESCRIPTION_VOICE',
        'Assess whether the student includes simple actions, feelings or descriptive details that make the composition clearer and more interesting.',
        10.00
    );

INSERT INTO rubric_criterion (
    language,
    difficulty_level,
    criterion_name,
    criterion_description,
    weight_percentage
)
VALUES
    (
        'ENGLISH',
        'INTERMEDIATE',
        'TASK_IDEAS',
        'Assess whether the student addresses the topic clearly and develops the main events with relevant supporting details, explanations and reactions appropriate for Primary 3–4.',
        25.00
    ),
    (
        'ENGLISH',
        'INTERMEDIATE',
        'ORGANISATION_COHESION',
        'Assess whether the composition has a clear beginning, development and ending, with logical paragraphing and suitable transitions between events and ideas.',
        20.00
    ),
    (
        'ENGLISH',
        'INTERMEDIATE',
        'GRAMMAR_SENTENCE_CONTROL',
        'Assess control of simple and compound sentences, tense consistency, subject-verb agreement and attempts at more varied sentence structures.',
        20.00
    ),
    (
        'ENGLISH',
        'INTERMEDIATE',
        'VOCABULARY_WORD_CHOICE',
        'Assess whether vocabulary is accurate, reasonably varied and appropriate to the context, with limited unnecessary repetition.',
        15.00
    ),
    (
        'ENGLISH',
        'INTERMEDIATE',
        'MECHANICS',
        'Assess spelling, punctuation, capitalisation and paragraph formatting. Errors should be considered according to their frequency and impact on readability.',
        10.00
    ),
    (
        'ENGLISH',
        'INTERMEDIATE',
        'EXPRESSION_DESCRIPTION_VOICE',
        'Assess the use of description, dialogue, actions and emotional detail to develop events and engage the reader.',
        10.00
    );

INSERT INTO rubric_criterion (
    language,
    difficulty_level,
    criterion_name,
    criterion_description,
    weight_percentage
)
VALUES
    (
        'ENGLISH',
        'ADVANCED',
        'TASK_IDEAS',
        'Assess whether the student produces a focused, relevant and well-developed response with purposeful elaboration, meaningful details and sufficient depth for Primary 5–6 or PSLE preparation.',
        25.00
    ),
    (
        'ENGLISH',
        'ADVANCED',
        'ORGANISATION_COHESION',
        'Assess whether the composition is deliberately structured with effective pacing, clear progression, coherent paragraphs and smooth transitions.',
        20.00
    ),
    (
        'ENGLISH',
        'ADVANCED',
        'GRAMMAR_SENTENCE_CONTROL',
        'Assess accurate and flexible control of varied sentence structures, tense, agreement, clauses and sentence boundaries. Errors should rarely affect fluency or meaning.',
        20.00
    ),
    (
        'ENGLISH',
        'ADVANCED',
        'VOCABULARY_WORD_CHOICE',
        'Assess whether vocabulary is precise, varied, natural and appropriate to the context. Do not reward difficult vocabulary when it is forced or incorrectly used.',
        15.00
    ),
    (
        'ENGLISH',
        'ADVANCED',
        'MECHANICS',
        'Assess accurate spelling, punctuation, capitalisation, paragraphing and dialogue punctuation where applicable.',
        10.00
    ),
    (
        'ENGLISH',
        'ADVANCED',
        'EXPRESSION_DESCRIPTION_VOICE',
        'Assess purposeful use of description, dialogue, reflection, emotional depth, personal voice and writing techniques to create reader impact.',
        10.00
    );

INSERT INTO rubric_criterion (
    language,
    difficulty_level,
    criterion_name,
    criterion_description,
    weight_percentage
)
VALUES
    (
        'CHINESE',
        'BEGINNER',
        'CONTENT_RELEVANCE',
        '评估学生是否能够围绕题目，写出简单、相关并容易理解的内容。对于小一至小二，应重视内容清楚和完整，不要求复杂的发展。',
        25.00
    ),
    (
        'CHINESE',
        'BEGINNER',
        'STRUCTURE_COHERENCE',
        '评估事情是否按照基本顺序表达，文章是否有简单的开头、经过和结尾。',
        20.00
    ),
    (
        'CHINESE',
        'BEGINNER',
        'GRAMMAR_SENTENCE_CONTROL',
        '评估句子是否完整通顺、语序是否基本正确，以及语法问题是否影响主要意思。',
        20.00
    ),
    (
        'CHINESE',
        'BEGINNER',
        'VOCABULARY_EXPRESSION',
        '评估学生是否能够正确使用常见、符合年龄程度的词语来表达人物、动作、地点和感受。',
        15.00
    ),
    (
        'CHINESE',
        'BEGINNER',
        'CHARACTERS_PUNCTUATION',
        '评估常用汉字、基本标点和书写规范。若错别字不影响理解，不应给予过重的扣分。',
        10.00
    ),
    (
        'CHINESE',
        'BEGINNER',
        'DESCRIPTION_EMOTION_VOICE',
        '评估学生是否加入简单的动作、感受或描写，使内容更加清楚和生动。',
        10.00
    );

INSERT INTO rubric_criterion (
    language,
    difficulty_level,
    criterion_name,
    criterion_description,
    weight_percentage
)
VALUES
    (
        'CHINESE',
        'INTERMEDIATE',
        'CONTENT_RELEVANCE',
        '评估学生是否切合题意，并以相关的细节、说明和人物反应发展主要事件，达到小三至小四的写作要求。',
        25.00
    ),
    (
        'CHINESE',
        'INTERMEDIATE',
        'STRUCTURE_COHERENCE',
        '评估文章是否有清楚的开头、发展和结尾，段落安排是否合理，以及事件和内容之间的衔接是否自然。',
        20.00
    ),
    (
        'CHINESE',
        'INTERMEDIATE',
        'GRAMMAR_SENTENCE_CONTROL',
        '评估句子是否完整通顺，语序和关联词是否恰当，并能尝试运用不同的句式。',
        20.00
    ),
    (
        'CHINESE',
        'INTERMEDIATE',
        'VOCABULARY_EXPRESSION',
        '评估词语是否准确、较为丰富并符合语境，同时避免不必要的重复。',
        15.00
    ),
    (
        'CHINESE',
        'INTERMEDIATE',
        'CHARACTERS_PUNCTUATION',
        '评估汉字书写、标点使用和分段是否正确。应根据错误出现的次数及其对阅读的影响进行评分。',
        10.00
    ),
    (
        'CHINESE',
        'INTERMEDIATE',
        'DESCRIPTION_EMOTION_VOICE',
        '评估学生是否运用人物描写、对话、动作和情感细节来发展情节并吸引读者。',
        10.00
    );

INSERT INTO rubric_criterion (
    language,
    difficulty_level,
    criterion_name,
    criterion_description,
    weight_percentage
)
VALUES
    (
        'CHINESE',
        'ADVANCED',
        'CONTENT_RELEVANCE',
        '评估文章是否切合题意、重点明确且内容充实，并能通过有意义的细节和深入的发挥达到小五至小六或小六会考备考的写作要求。',
        25.00
    ),
    (
        'CHINESE',
        'ADVANCED',
        'STRUCTURE_COHERENCE',
        '评估文章结构是否经过周密安排，情节节奏是否恰当，段落是否连贯，以及内容推进和衔接是否自然流畅。',
        20.00
    ),
    (
        'CHINESE',
        'ADVANCED',
        'GRAMMAR_SENTENCE_CONTROL',
        '评估学生是否能准确、灵活地运用多种句式、恰当的语序和关联词。语法错误不应经常影响文章的流畅度或意思。',
        20.00
    ),
    (
        'CHINESE',
        'ADVANCED',
        'VOCABULARY_EXPRESSION',
        '评估用词是否准确、丰富、自然并符合语境。不应奖励生硬、牵强或使用错误的难词。',
        15.00
    ),
    (
        'CHINESE',
        'ADVANCED',
        'CHARACTERS_PUNCTUATION',
        '评估汉字、标点、分段以及对话标点的使用是否准确、规范。',
        10.00
    ),
    (
        'CHINESE',
        'ADVANCED',
        'DESCRIPTION_EMOTION_VOICE',
        '评估学生是否有目的地运用描写、对话、反思、情感层次、个人风格和写作手法，以增强文章对读者的感染力。',
        10.00
    );

UPDATE rubric_criterion
SET created_by = 'SYSTEM',
    updated_by = 'SYSTEM'
WHERE created_by IS NULL
  AND updated_by IS NULL;
