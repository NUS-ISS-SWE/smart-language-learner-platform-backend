package com.nus.iss.controller;

import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.AiUsage;
import com.nus.iss.dto.CriterionEvidence;
import com.nus.iss.dto.EvidenceCollectionResult;
import com.nus.iss.dto.EvidenceItem;
import com.nus.iss.dto.RubricCriterionDTO;
import com.nus.iss.enums.EvidencePolarity;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import com.nus.iss.exception.EvidenceGenerationException;
import com.nus.iss.service.impl.EvidenceCollectionServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for the A2A adapter; evidence collection itself remains mocked. */
@WebMvcTest(EvidenceA2aController.class)
class EvidenceA2aControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EvidenceCollectionServiceImpl service;

    @MockitoBean
    private SllpProperties properties;

    @Test
    void advertisesTheEvidenceCapabilityAndReturnsACompletedArtifact() throws Exception {
        when(properties.getPublicBaseUrl()).thenReturn("http://evidence.test");
        when(properties.getA2aToken()).thenReturn("test-token");
        when(service.collect(argThat(request -> request.compositionId().equals(12L)
                && request.userId().equals(7L) && request.language().name().equals("ENGLISH")), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(new EvidenceCollectionResult(12L, List.of("Read the composition."), List.of(
                        new CriterionEvidence("TASK_IDEAS", List.of(new EvidenceItem("I went home.", "paragraph 1, characters 1-12", EvidencePolarity.STRENGTH,
                                "The event is clear.", true)))), List.of(new RubricCriterionDTO(1L, Language.ENGLISH,
                        DifficultyLevel.BEGINNER, "TASK_IDEAS", "Develop clear ideas.", new BigDecimal("100"),
                        "test", "test", null, null)), usage()));

        mockMvc.perform(get("/.well-known/agent-card.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skills[0].id").value("evidence-collection"))
                .andExpect(jsonPath("$.sllp.contractVersion").value("2.0"));

        mockMvc.perform(post("/a2a").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer test-token").content(requestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.task.status.state").value("TASK_STATE_COMPLETED"))
                .andExpect(jsonPath("$.result.task.artifacts[0].parts[0].data.criteria[0].criterion").value("TASK_IDEAS"))
                .andExpect(jsonPath("$.result.task.artifacts[0].parts[0].data.rubricSnapshot.criteria[0].name").value("TASK_IDEAS"));

        verify(service).collect(argThat(request -> request.compositionId().equals(12L)), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsAnUnauthenticatedA2aCall() throws Exception {
        when(properties.getA2aToken()).thenReturn("test-token");

        mockMvc.perform(post("/a2a").contentType(MediaType.APPLICATION_JSON).content(requestBody()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void rejectsAMalformedRequestWithoutARetryableStatus() throws Exception {
        when(properties.getA2aToken()).thenReturn("test-token");

        mockMvc.perform(post("/a2a").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer test-token")
                        .content(requestBody().replace("\"ENGLISH\"", "\"KLINGON\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error.code").value(-32602));

        verifyNoInteractions(service);
    }

    @Test
    void reportsAnExecutionFailureAsRetryableServerError() throws Exception {
        when(properties.getA2aToken()).thenReturn("test-token");
        when(service.collect(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new EvidenceGenerationException("Evidence agent failed: model call failed"));

        mockMvc.perform(post("/a2a").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer test-token").content(requestBody()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value(-32603));
    }

    private static AiUsage usage() {
        return new AiUsage(10, 2, 0, 12, 1, "OPENAI", "test-model", "test-v1", 1, 1, 1);
    }

    private static String requestBody() {
        return """
                {"jsonrpc":"2.0","id":"request-1","method":"SendMessage","params":{"message":{"contextId":"workflow-1","parts":[{"mediaType":"application/json","data":{"composition":{"compositionId":12,"userId":7,"title":"A day","content":"I went home.","language":"ENGLISH"}}}]}}}
                """;
    }
}
