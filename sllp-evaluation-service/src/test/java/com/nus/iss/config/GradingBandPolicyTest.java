package com.nus.iss.config;

import com.nus.iss.dto.AdminRubricCriterionDTO;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradingBandPolicyTest {
    private static final List<String> CRITERIA = List.of(
            "TASK_IDEAS", "ORGANISATION_COHESION", "GRAMMAR_SENTENCE_CONTROL",
            "VOCABULARY_WORD_CHOICE", "MECHANICS", "EXPRESSION_DESCRIPTION_VOICE");

    @Test
    void definesAllFiveBandsForEveryCriterionAtEveryDifficulty() {
        for (DifficultyLevel difficulty : DifficultyLevel.values()) {
            String descriptors = GradingBandPolicy.descriptors(difficulty, rubric(difficulty));
            for (String criterion : CRITERIA) {
                assertTrue(descriptors.contains(criterion + ":"));
            }
            for (int band = 1; band <= 5; band++) {
                assertTrue(descriptors.contains("Band " + band + ":"));
            }
        }
    }

    @Test
    void advancedPolicyIsConcreteAndNotAnEasierLevelCopy() {
        String beginner = GradingBandPolicy.descriptors(
                DifficultyLevel.BEGINNER, rubric(DifficultyLevel.BEGINNER));
        String advanced = GradingBandPolicy.descriptors(
                DifficultyLevel.ADVANCED, rubric(DifficultyLevel.ADVANCED));

        assertAll(
                () -> assertTrue(advanced.contains("purposeful and well-developed response")),
                () -> assertTrue(advanced.contains("effective pacing")),
                () -> assertTrue(advanced.contains("flexible control of varied sentence structures")),
                () -> assertTrue(advanced.contains("precise, varied and natural vocabulary")),
                () -> assertTrue(advanced.contains("emotional depth")),
                () -> assertFalse(beginner.contains("effective pacing"))
        );
    }

    private static List<AdminRubricCriterionDTO> rubric(DifficultyLevel difficulty) {
        return java.util.stream.IntStream.range(0, CRITERIA.size())
                .mapToObj(index -> new AdminRubricCriterionDTO(
                        (long) index + 1, Language.ENGLISH, difficulty, CRITERIA.get(index),
                        "Admin description", new BigDecimal("16.67"),
                        "SYSTEM", "SYSTEM", null, null))
                .toList();
    }
}
