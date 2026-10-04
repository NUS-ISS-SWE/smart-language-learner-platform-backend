package com.nus.iss.service.impl;

import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.A2aRequestDTO;
import com.nus.iss.dto.AiCriterionAssessmentDTO;
import com.nus.iss.dto.AiGradingOutputDTO;
import com.nus.iss.dto.EvidenceDTO;
import com.nus.iss.dto.GradingRequestDTO;
import com.nus.iss.dto.WorkflowRubricCriterionDTO;
import com.nus.iss.enums.Language;
import com.nus.iss.exception.A2aInvalidRequestException;
import com.nus.iss.exception.A2aUnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ValidationServiceImplTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ValidationServiceImpl validation = new ValidationServiceImpl(new SllpProperties(), objectMapper);

    @Test
    void extractsTypedA2aRequestWithRubricSnapshot() throws Exception {
        A2aRequestDTO input = objectMapper.readValue("""
                {"jsonrpc":"2.0","id":"request-1","method":"SendMessage","params":{"message":{"contextId":"workflow-1","parts":[{"mediaType":"application/json","data":{"composition":{"compositionId":9,"userId":7,"title":"A day","content":"I went home.","language":"ENGLISH"},"evidence":{"criteria":[{"evidence":[{"excerpt":"I went home."}]}]},"rubricSnapshot":{"version":"rubric-v1","criteria":[{"name":"TASK_IDEAS","weight":100,"guidance":"Develop ideas."}]}}}]}}}
                """, A2aRequestDTO.class);

        GradingRequestDTO result = validation.validateAndExtract(input);

        assertEquals(9L, result.getCompositionId());
        assertEquals(List.of("I went home."), result.getEvidence());
        assertEquals("rubric-v1", result.getRubricVersion());
    }

    @Test
    void validatesBearerTokenWithoutExternalAuthentication() {
        SllpProperties properties = new SllpProperties();
        properties.setA2aToken("expected-token");
        ValidationServiceImpl secured = new ValidationServiceImpl(properties, objectMapper);

        assertDoesNotThrow(() -> secured.validateAuthorization("Bearer expected-token"));
        assertThrows(A2aUnauthorizedException.class, () -> secured.validateAuthorization(null));
        assertThrows(A2aUnauthorizedException.class, () -> secured.validateAuthorization("expected-token"));
        assertThrows(A2aUnauthorizedException.class, () -> secured.validateAuthorization("Bearer wrong-token"));
    }

    @Test
    void rejectsAuthorizationWhenConfiguredTokenIsMissingOrBlank() {
        SllpProperties missing = new SllpProperties();
        SllpProperties blank = new SllpProperties();
        blank.setA2aToken(" ");

        assertThrows(A2aUnauthorizedException.class,
                () -> new ValidationServiceImpl(missing, objectMapper).validateAuthorization("Bearer token"));
        assertThrows(A2aUnauthorizedException.class,
                () -> new ValidationServiceImpl(blank, objectMapper).validateAuthorization("Bearer token"));
    }

    @Test
    void rejectsNullAndIncompleteA2aRequestsWithSafeRequestIds() {
        A2aInvalidRequestException nullRequest = assertThrows(A2aInvalidRequestException.class,
                () -> validation.validateAndExtract(null));
        A2aRequestDTO incomplete = A2aRequestDTO.builder().id("request-2").jsonrpc("1.0").build();
        A2aInvalidRequestException invalid = assertThrows(A2aInvalidRequestException.class,
                () -> validation.validateAndExtract(incomplete));

        assertNotNull(nullRequest.getRequestId());
        assertEquals("request-2", invalid.getRequestId());
    }

    @Test
    void rejectsUnsupportedLanguageAndGeneratesIdWhenInputIdIsBlank() throws Exception {
        A2aRequestDTO input = objectMapper.readValue("""
                {"jsonrpc":"2.0","id":" ","method":"SendMessage","params":{"message":{"contextId":"workflow-1","parts":[{"mediaType":"application/json","data":{"composition":{"userId":7,"title":"A day","content":"Text","language":"KLINGON"},"evidence":{"criteria":[]}}}]}}}
                """, A2aRequestDTO.class);

        A2aInvalidRequestException error = assertThrows(A2aInvalidRequestException.class,
                () -> validation.validateAndExtract(input));

        assertNotNull(error.getRequestId());
        assertTrueUuid(error.getRequestId());
    }

    @Test
    void wrapsUnexpectedNormalizationFailures() throws Exception {
        A2aRequestDTO input = objectMapper.readValue("""
                {"jsonrpc":"2.0","id":"request-3","method":"SendMessage","params":{"message":{"contextId":"workflow-1","parts":[{"mediaType":"application/json","data":{"composition":{"userId":7,"title":"A day","content":"Text","language":"ENGLISH"},"evidence":{"criteria":[]},"rubricSnapshot":{"version":"rubric-v1","criteria":[{"name":"TASK_IDEAS","weight":100,"guidance":"Develop ideas."}]}}}]}}}
                """, A2aRequestDTO.class);
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any())).thenThrow(new RuntimeException("serialization failed"));

        A2aInvalidRequestException error = assertThrows(A2aInvalidRequestException.class,
                () -> new ValidationServiceImpl(new SllpProperties(), failingMapper).validateAndExtract(input));

        assertEquals("request-3", error.getRequestId());
        assertNotNull(error.getCause());
    }

    @Test
    void acceptsValidWorkflowRubricAndCompleteGroundedOutput() {
        assertDoesNotThrow(() -> validation.validateRubric(rubric()));
        assertDoesNotThrow(() -> validation.validateAiOutput(
                output("I went home.", 3), request(), rubric()));
    }

    @Test
    void rejectsEmptyInvalidDuplicateAndUnbalancedRubrics() {
        assertThrows(IllegalStateException.class,
                () -> validation.validateRubric(null));
        assertThrows(IllegalStateException.class,
                () -> validation.validateRubric(List.of()));
        assertThrows(IllegalStateException.class,
                () -> validation.validateRubric(java.util.Collections.singletonList(null)));

        WorkflowRubricCriterionDTO half = new WorkflowRubricCriterionDTO(
                "TASK_IDEAS", new BigDecimal("50"), "Develop ideas.");
        assertThrows(IllegalStateException.class,
                () -> validation.validateRubric(List.of(half)));
        assertThrows(IllegalStateException.class,
                () -> validation.validateRubric(List.of(half, half)));
    }

    @Test
    void rejectsMalformedAiEnvelopeAndRubricCoverageMismatch() {
        assertThrows(IllegalStateException.class,
                () -> validation.validateAiOutput(null, request(), rubric()));
        assertThrows(IllegalStateException.class,
                () -> validation.validateAiOutput(AiGradingOutputDTO.builder().confidence(Double.NaN).build(),
                        request(), rubric()));
        assertThrows(IllegalStateException.class,
                () -> validation.validateAiOutput(AiGradingOutputDTO.builder().confidence(1.1).criteria(List.of()).build(),
                        request(), rubric()));

        AiGradingOutputDTO wrongCriterion = AiGradingOutputDTO.builder().confidence(0.5).criteria(List.of(
                AiCriterionAssessmentDTO.builder().criterion("LANGUAGE_USE").bandScore(3)
                        .rationale("Grounded rationale").evidence(List.of(
                                new EvidenceDTO("I went home.", null))).build())).build();
        assertThrows(IllegalStateException.class,
                () -> validation.validateAiOutput(wrongCriterion, request(), rubric()));
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

    private static List<WorkflowRubricCriterionDTO> rubric() {
        return List.of(new WorkflowRubricCriterionDTO(
                "TASK_IDEAS", new BigDecimal("100"), "Develop ideas."));
    }

    private static void assertTrueUuid(String value) {
        assertDoesNotThrow(() -> java.util.UUID.fromString(value));
    }
}
