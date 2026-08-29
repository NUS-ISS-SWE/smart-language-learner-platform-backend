package com.nus.iss.agent;

import com.nus.iss.dto.CompositionEvaluationResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GradingAgentServiceTest {

    @Test
    void formatsEvidenceForGroundedGrading() {
        assertEquals("""
                1. Grammar: subject-verb disagreement in paragraph 1.
                2. Vocabulary: repeated use of 'good'.""",
                GradingAgentService.formatEvidence(List.of(
                        "Grammar: subject-verb disagreement in paragraph 1.",
                        "Vocabulary: repeated use of 'good'.")));
    }

    @Test
    void normalizesScoreAndFeedback() {
        CompositionEvaluationResult normalized = GradingAgentService.normalizeResult(
                new CompositionEvaluationResult(new BigDecimal("105.126"), "  Useful feedback  "));

        assertEquals(new BigDecimal("100.00"), normalized.score());
        assertEquals("Useful feedback", normalized.feedback());
    }

    @Test
    void rejectsMissingAiFeedback() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> GradingAgentService.normalizeResult(
                        new CompositionEvaluationResult(new BigDecimal("70"), " ")));

        assertEquals("AI grading returned no feedback", exception.getMessage());
    }
}
