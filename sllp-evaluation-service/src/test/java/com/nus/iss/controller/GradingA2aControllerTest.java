package com.nus.iss.controller;

import com.nus.iss.dto.A2aRequestDTO;
import com.nus.iss.dto.AiUsage;
import com.nus.iss.dto.CriterionAssessmentDTO;
import com.nus.iss.dto.EvidenceDTO;
import com.nus.iss.dto.GradingRequestDTO;
import com.nus.iss.dto.GradingResultDTO;
import com.nus.iss.enums.Language;
import com.nus.iss.exception.A2aExecutionException;
import com.nus.iss.exception.A2aUnauthorizedException;
import com.nus.iss.service.GradingService;
import com.nus.iss.service.ValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GradingA2aController.class)
class GradingA2aControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private ValidationService validationService;
    @MockitoBean private GradingService gradingService;

    @Test
    void delegatesTypedRequestAndReturnsStructuredAssessment() throws Exception {
        GradingRequestDTO command = command();
        when(validationService.validateAndExtract(any(A2aRequestDTO.class))).thenReturn(command);
        when(gradingService.grade(command)).thenReturn(result());

        mockMvc.perform(post("/a2a").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer test-token").content(requestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("request-1"))
                .andExpect(jsonPath("$.result.task.id").value("task-1"))
                .andExpect(jsonPath("$.result.task.status.state").value("TASK_STATE_COMPLETED"))
                .andExpect(jsonPath("$.result.task.artifacts[0].parts[0].data.criteria[0].criterion").value("TASK_IDEAS"))
                .andExpect(jsonPath("$.result.task.artifacts[0].parts[0].data.usage.totalTokens").value(12))
                .andExpect(jsonPath("$.result.task.artifacts[0].parts[0].data.usage.estimatedCostMicros").doesNotExist());

        verify(validationService).validateAuthorization("Bearer test-token");
        verify(gradingService).grade(command);
    }

    @Test
    void rejectsUnauthenticatedCallBeforeGrading() throws Exception {
        doThrow(new A2aUnauthorizedException()).when(validationService).validateAuthorization(null);
        mockMvc.perform(post("/a2a").contentType(MediaType.APPLICATION_JSON).content(requestBody()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(gradingService);
    }

    @Test
    void mapsExecutionFailureToRetriableJsonRpcError() throws Exception {
        when(validationService.validateAndExtract(any(A2aRequestDTO.class))).thenReturn(command());
        when(gradingService.grade(any())).thenThrow(new A2aExecutionException("request-1", "failed"));
        mockMvc.perform(post("/a2a").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer test-token").content(requestBody()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.id").value("request-1"))
                .andExpect(jsonPath("$.error.code").value(-32603))
                .andExpect(jsonPath("$.error.message").value("grading execution failed"));
    }

    private static GradingRequestDTO command() {
        return GradingRequestDTO.builder().requestId("request-1").contextId("workflow-1")
                .userId(7L).title("A day").content("I went home.").language(Language.ENGLISH)
                .evidence(List.of("I went home.")).requestHash("hash").build();
    }

    private static GradingResultDTO result() {
        CriterionAssessmentDTO assessment = new CriterionAssessmentDTO("TASK_IDEAS", new BigDecimal("72.00"),
                "The event is clear.", List.of(new EvidenceDTO("I went home.", "paragraph 1")));
        AiUsage usage = new AiUsage(10, 2, 0, 12, 1L, "OPENAI", "test-model", "test-v1", 1, 1, 1);
        return GradingResultDTO.builder().taskId("task-1").artifactId("artifact-1")
                .overallScore(new BigDecimal("72.00")).rubricScores(Map.of("TASK_IDEAS", new BigDecimal("72.00")))
                .criteria(List.of(assessment)).confidence(0.9).rubricVersion("test-v1")
                .modelVersion("test-model").promptVersion("grading-v3").usage(usage).build();
    }

    private static String requestBody() {
        return """
                {"jsonrpc":"2.0","id":"request-1","method":"SendMessage","params":{"message":{"contextId":"workflow-1","parts":[{"mediaType":"application/json","data":{"composition":{"userId":7,"title":"A day","content":"I went home.","language":"ENGLISH"},"evidence":{"criteria":[{"evidence":[{"excerpt":"I went home."}]}],"rubricSnapshot":{"version":"test-v1","criteria":[{"name":"TASK_IDEAS","weight":100,"guidance":"Develop ideas."}]}}}}]}}}
                """;
    }
}
