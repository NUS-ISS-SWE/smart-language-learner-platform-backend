package com.nus.iss.service.impl;

import com.nus.iss.dto.CompositionGradingRequestedEvent;
import com.nus.iss.dto.RubricCriterionDTO;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompositionEvaluationServiceImplTest {

    @Test
    void gradingEventDeserializesUserIdWithoutUsername() throws JacksonException {
        String payload = "{\"compositionId\":10,\"userId\":1,\"title\":\"My title\","
                + "\"content\":\"My composition\",\"language\":\"ENGLISH\"}";

        CompositionGradingRequestedEvent event = new ObjectMapper()
                .readValue(payload, CompositionGradingRequestedEvent.class);

        assertEquals(1L, event.getUserId());
        assertFalse(new ObjectMapper().writeValueAsString(event).contains("username"));
    }

    @Test
    void guidanceVariesByUserDifficultyLevel() {
        String beginner = CompositionEvaluationServiceImpl.guidanceFor(DifficultyLevel.BEGINNER);
        String intermediate = CompositionEvaluationServiceImpl.guidanceFor(DifficultyLevel.INTERMEDIATE);
        String advanced = CompositionEvaluationServiceImpl.guidanceFor(DifficultyLevel.ADVANCED);

        assertTrue(beginner.contains("beginner expectations"));
        assertTrue(intermediate.contains("intermediate expectations"));
        assertTrue(advanced.contains("advanced expectations"));
    }

    @Test
    void formatsAdminRubricCriteriaForTheEvaluationPrompt() {
        List<RubricCriterionDTO> criteria = List.of(
                new RubricCriterionDTO(
                        1L,
                        Language.ENGLISH,
                        DifficultyLevel.INTERMEDIATE,
                        "TASK_IDEAS",
                        "Assess relevant and developed ideas.",
                        new BigDecimal("25.00"),
                        null,
                        null,
                        null,
                        null),
                new RubricCriterionDTO(
                        2L,
                        Language.ENGLISH,
                        DifficultyLevel.INTERMEDIATE,
                        "MECHANICS",
                        "Assess spelling and punctuation.",
                        new BigDecimal("10.00"),
                        null,
                        null,
                        null,
                        null));

        String rubric = CompositionEvaluationServiceImpl.formatRubric(criteria);

        assertEquals("""
                - TASK_IDEAS (25%): Assess relevant and developed ideas.
                - MECHANICS (10%): Assess spelling and punctuation.""", rubric);
    }
}
