package com.nus.iss.config;

import com.nus.iss.dto.WorkflowRubricCriterionDTO;
import com.nus.iss.enums.DifficultyLevel;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Produces explicit, level-relative band descriptors for every rubric criterion.
 */
public final class GradingBandPolicy {
    private static final Map<DifficultyLevel, Map<String, String>> EXPECTATIONS = Map.of(
            DifficultyLevel.BEGINNER, Map.of(
                    "TASK_IDEAS", "a simple, relevant and understandable response with a recognisable event sequence",
                    "ORGANISATION_COHESION", "a basic beginning, middle and ending in logical order, using simple connectors",
                    "GRAMMAR_SENTENCE_CONTROL", "understandable, mostly complete simple sentences whose errors do not obscure meaning",
                    "VOCABULARY_WORD_CHOICE", "accurate familiar words for actions, people, places and feelings",
                    "MECHANICS", "readable basic spelling, capitals and sentence-ending punctuation",
                    "EXPRESSION_DESCRIPTION_VOICE", "simple actions, feelings or details that make the writing clear and interesting"),
            DifficultyLevel.INTERMEDIATE, Map.of(
                    "TASK_IDEAS", "a clearly developed response with relevant supporting details, explanations and reactions",
                    "ORGANISATION_COHESION", "a clear beginning, development and ending with logical paragraphs and suitable transitions",
                    "GRAMMAR_SENTENCE_CONTROL", "controlled simple and compound sentences, consistent tense and agreement, with some structural variety",
                    "VOCABULARY_WORD_CHOICE", "accurate, reasonably varied contextual vocabulary with limited unnecessary repetition",
                    "MECHANICS", "generally accurate spelling, punctuation, capitals and paragraph formatting",
                    "EXPRESSION_DESCRIPTION_VOICE", "developed description, dialogue, action or emotion that engages the reader"),
            DifficultyLevel.ADVANCED, Map.of(
                    "TASK_IDEAS", "a focused, purposeful and well-developed response with meaningful elaboration and depth",
                    "ORGANISATION_COHESION", "deliberate structure, effective pacing, coherent paragraphs, clear progression and smooth transitions",
                    "GRAMMAR_SENTENCE_CONTROL", "accurate, flexible control of varied sentence structures, clauses, tense, agreement and boundaries",
                    "VOCABULARY_WORD_CHOICE", "precise, varied and natural vocabulary used effectively without forced complexity",
                    "MECHANICS", "consistently accurate spelling, punctuation, capitals, paragraphing and applicable dialogue punctuation",
                    "EXPRESSION_DESCRIPTION_VOICE", "purposeful and sustained description, reflection, emotional depth, personal voice and reader impact")
    );

    private GradingBandPolicy() {
    }

    public static String descriptors(DifficultyLevel difficultyLevel,
                                     List<WorkflowRubricCriterionDTO> rubric) {
        Map<String, String> expectations = EXPECTATIONS.get(difficultyLevel);
        return rubric.stream()
                .map(criterion -> describeCriterion(criterion, expectations))
                .collect(Collectors.joining("\n\n"));
    }

    private static String describeCriterion(WorkflowRubricCriterionDTO criterion,
                                            Map<String, String> expectations) {
        String expectation = expectations.getOrDefault(
                criterion.name(), criterion.guidance());
        return """
                %s:
                - Band 1: Shows little or no evidence of %s; major weaknesses dominate.
                - Band 2: Shows some evidence of %s, but it is limited or inconsistent and weaknesses outweigh strengths.
                - Band 3: Generally demonstrates %s, although noticeable weaknesses or inconsistencies remain.
                - Band 4: Demonstrates %s consistently, with only occasional minor weaknesses.
                - Band 5: Demonstrates %s exceptionally and consistently throughout, with almost no meaningful weaknesses.
                """.formatted(criterion.name(), expectation, expectation, expectation,
                expectation, expectation).strip();
    }
}
