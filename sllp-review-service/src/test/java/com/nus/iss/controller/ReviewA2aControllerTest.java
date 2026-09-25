package com.nus.iss.controller;

import com.nus.iss.agent.ReviewAgentService;
import com.nus.iss.agent.ReviewModelResponse;
import com.nus.iss.config.SllpProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for Review's A2A endpoint, independent of the LLM provider. */
@WebMvcTest(ReviewA2aController.class)
class ReviewA2aControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewAgentService service;

    @MockitoBean
    private SllpProperties properties;

    @Test
    void advertisesTheReviewCapabilityAndCompletesAnApprovedReview() throws Exception {
        when(properties.getPublicBaseUrl()).thenReturn("http://review.test");
        when(properties.getA2aToken()).thenReturn("test-token");
        when(service.review(anyString(), eq(7L), eq("ENGLISH"))).thenReturn(new ReviewAgentService.Reviewed(
                new ReviewModelResponse("APPROVE", 0.9, List.of(new ReviewModelResponse.Finding("TASK_IDEAS", "Evidence supports the score."))),
                Map.of("provider", "OPENAI", "model", "test-model", "promptTokens", 10L, "cachedInputTokens", 0L,
                        "completionTokens", 2L, "totalTokens", 12L, "pricingVersion", "test-v1",
                        "inputPriceMicrosPerMillion", 1L, "cachedInputPriceMicrosPerMillion", 1L, "outputPriceMicrosPerMillion", 1L)));

        mockMvc.perform(get("/.well-known/agent-card.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skills[0].id").value("assessment-review"));

        mockMvc.perform(post("/a2a").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer test-token").content(requestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.task.status.state").value("TASK_STATE_COMPLETED"))
                .andExpect(jsonPath("$.result.task.artifacts[0].parts[0].data.decision").value("APPROVE"))
                .andExpect(jsonPath("$.result.task.artifacts[0].parts[0].data.findings[0].criterion").value("TASK_IDEAS"));

        verify(service).review(anyString(), eq(7L), eq("ENGLISH"));
    }

    @Test
    void rejectsAnUnauthenticatedA2aCall() throws Exception {
        when(properties.getA2aToken()).thenReturn("test-token");

        mockMvc.perform(post("/a2a").contentType(MediaType.APPLICATION_JSON).content(requestBody()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void autoApprovesOnlyWhenTheDevelopmentSwitchIsEnabled() throws Exception {
        when(properties.getA2aToken()).thenReturn("test-token");
        when(properties.isReviewAutoApprove()).thenReturn(true);

        mockMvc.perform(post("/a2a").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer test-token").content(requestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.task.artifacts[0].parts[0].data.decision").value("APPROVE"))
                .andExpect(jsonPath("$.result.task.artifacts[0].parts[0].data.promptVersion")
                        .value("review-auto-approve-local-v1"));

        verifyNoInteractions(service);
    }

    @Test
    void reportsAnExecutionFailureAsRetriableInsteadOfInvalidInput() throws Exception {
        when(properties.getA2aToken()).thenReturn("test-token");
        when(service.review(anyString(), eq(7L), eq("ENGLISH")))
                .thenThrow(new IllegalStateException("model unavailable"));

        mockMvc.perform(post("/a2a").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer test-token").content(requestBody()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value(-32603));
    }

    private static String requestBody() {
        return """
                {"jsonrpc":"2.0","id":"request-1","method":"SendMessage","params":{"message":{"contextId":"workflow-1","parts":[{"mediaType":"application/json","data":{"composition":{"userId":7,"language":"ENGLISH"},"grading":{"overallScore":72}}}]}}}
                """;
    }
}
