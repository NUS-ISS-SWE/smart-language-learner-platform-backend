package com.nus.iss.service.impl;

import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.A2aRequestDTO;
import com.nus.iss.dto.AdminRubricCriterionDTO;
import com.nus.iss.dto.AiCriterionAssessmentDTO;
import com.nus.iss.dto.AiGradingOutputDTO;
import com.nus.iss.dto.EvidenceDTO;
import com.nus.iss.dto.GradingRequestDTO;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidationServiceImplTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ValidationServiceImpl validation = new ValidationServiceImpl(new SllpProperties(), objectMapper);

    @Test
    void extractsTypedA2aRequestWithoutRubricSnapshot() throws Exception {
        A2aRequestDTO input = objectMapper.readValue("""
                {"jsonrpc":"2.0","id":"request-1","method":"SendMessage","params":{"message":{"contextId":"workflow-1","parts":[{"mediaType":"application/json","data":{"composition":{"compositionId":9,"userId":7,"title":"A day","content":"I went home.","language":"ENGLISH"},"evidence":{"criteria":[{"evidence":[{"excerpt":"I went home."}]}]}}}]}}}
                """, A2aRequestDTO.class);

        GradingRequestDTO result = validation.validateAndExtract(input);

        assertEquals(9L, result.getCompositionId());
        assertEquals(List.of("I went home."), result.getEvidence());
    }

    @Test
    void acceptsValidAdminRubricAndCompleteGroundedOutput() {
        assertDoesNotThrow(() -> validation.validateRubric(
                rubric(), Language.ENGLISH, DifficultyLevel.INTERMEDIATE));
        assertDoesNotThrow(() -> validation.validateAiOutput(
                output("I went home.", 3), request(), rubric()));
    }

    @Test
    void rejectsEvidenceInventedByTheModel() {
        assertThrows(IllegalStateException.class,
                () -> validation.validateAiOutput(
                        output("I flew to the moon.", 3), request(), rubric()));
    }

    @Test
    void acceptsGroundedEvidenceAcrossHtmlCaseAndWhitespaceDifferencesWithoutLocation() {
        GradingRequestDTO htmlRequest = GradingRequestDTO.builder()
                .requestId("request-1").contextId("workflow-1").userId(7L).title("A day")
                .content("<p>I went<br>home &amp; rested.</p>").language(Language.ENGLISH)
                .evidence(List.of()).requestHash("hash").build();
        AiGradingOutputDTO output = AiGradingOutputDTO.builder().confidence(0.9).criteria(List.of(
                AiCriterionAssessmentDTO.builder().criterion("TASK_IDEAS").bandScore(3)
                        .rationale("The action is clear.")
                        .evidence(List.of(new EvidenceDTO("i went home & rested.", null))).build())).build();

        assertDoesNotThrow(() -> validation.validateAiOutput(output, htmlRequest, rubric()));
    }

    @Test
    void discardsUngroundedExcerptWhenCriterionStillHasGroundedEvidence() {
        AiGradingOutputDTO output = AiGradingOutputDTO.builder().confidence(0.9).criteria(List.of(
                AiCriterionAssessmentDTO.builder().criterion("TASK_IDEAS").bandScore(3)
                        .rationale("The action is clear.")
                        .evidence(new java.util.ArrayList<>(List.of(
                                new EvidenceDTO("I went home.", "paragraph 1"),
                                new EvidenceDTO("I flew to the moon.", "paragraph 2"))))
                        .build())).build();

        assertDoesNotThrow(() -> validation.validateAiOutput(output, request(), rubric()));
        assertEquals(List.of(new EvidenceDTO("I went home.", "paragraph 1")),
                output.getCriteria().getFirst().getEvidence());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6})
    void rejectsBandScoresOutsideOneToFive(int bandScore) {
        assertThrows(IllegalStateException.class,
                () -> validation.validateAiOutput(
                        output("I went home.", bandScore), request(), rubric()));
    }

    private static AiGradingOutputDTO output(String excerpt, int bandScore) {
        return AiGradingOutputDTO.builder().confidence(0.9).criteria(List.of(
                AiCriterionAssessmentDTO.builder().criterion("TASK_IDEAS").bandScore(bandScore)
                        .rationale("The action is clear.")
                        .evidence(List.of(new EvidenceDTO(excerpt, "paragraph 1"))).build())).build();
    }

    private static GradingRequestDTO request() {
        return GradingRequestDTO.builder().requestId("request-1").contextId("workflow-1")
                .userId(7L).title("A day").content("I went home.").language(Language.ENGLISH)
                .evidence(List.of()).requestHash("hash").build();
    }

    private static List<AdminRubricCriterionDTO> rubric() {
        return List.of(new AdminRubricCriterionDTO(7L, Language.ENGLISH, DifficultyLevel.INTERMEDIATE,
                "TASK_IDEAS", "Develop ideas.", new BigDecimal("100"),
                "SYSTEM", "SYSTEM", null, null));
    }
}
