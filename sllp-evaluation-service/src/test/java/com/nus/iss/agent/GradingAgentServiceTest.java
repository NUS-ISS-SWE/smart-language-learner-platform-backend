package com.nus.iss.agent;

import com.nus.iss.dto.CompositionEvaluationResult;
import com.nus.iss.dto.A2aGradingResult;
import com.nus.iss.dto.A2aModelGradingResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GradingAgentServiceTest {

    private static final GradingAgentRequest.RubricSnapshot SNAPSHOT = new GradingAgentRequest.RubricSnapshot(
            "test-v1", List.of(
            new GradingAgentRequest.RubricSnapshot.Criterion("Ideas", new BigDecimal("60"), "Develop ideas."),
            new GradingAgentRequest.RubricSnapshot.Criterion("Grammar", new BigDecimal("40"), "Use correct grammar.")));

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

    @Test
    void rejectsStructuredGradingThatOmitsACanonicalCriterion() {
        A2aModelGradingResponse response = response(new BigDecimal("70"), Map.of("Ideas", new BigDecimal("70")),
                List.of(assessment("Ideas", "70")));

        assertThrows(IllegalStateException.class,
                () -> GradingAgentService.validateAndCalculateOverallScore(response, SNAPSHOT));
    }

    @Test
    void calculatesTheCanonicalOverallScoreInsteadOfTrustingModelArithmetic() {
        A2aModelGradingResponse response = response(new BigDecimal("70"), Map.of("Ideas", new BigDecimal("80"), "Grammar", new BigDecimal("60")),
                List.of(assessment("Ideas", "80"), assessment("Grammar", "60")));

        assertEquals(new BigDecimal("72.00"),
                GradingAgentService.validateAndCalculateOverallScore(response, SNAPSHOT));
    }

    private static A2aModelGradingResponse response(BigDecimal overall, Map<String, BigDecimal> scores,
                                                      List<A2aGradingResult.CriterionAssessment> criteria) {
        return new A2aModelGradingResponse(overall, scores, criteria, 0.9);
    }

    private static A2aGradingResult.CriterionAssessment assessment(String criterion, String score) {
        return new A2aGradingResult.CriterionAssessment(criterion, new BigDecimal(score), "Grounded rationale.",
                List.of(new A2aGradingResult.Evidence("Original text", "paragraph 1")));
    }
}
