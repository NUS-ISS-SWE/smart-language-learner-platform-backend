package com.nus.iss.controller;

import com.nus.iss.agent.GradingAgentService;
import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.A2aGradingResult;
import com.nus.iss.dto.AiUsage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract tests for Grading's synchronous A2A endpoint. */
@WebMvcTest(GradingA2aController.class)
class GradingA2aControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GradingAgentService service;

    @MockitoBean
    private SllpProperties properties;

    @Test
    void advertisesTheGradingCapabilityAndReturnsTheStructuredAssessment() throws Exception {
        when(properties.getPublicBaseUrl()).thenReturn("http://grading.test");
        when(properties.getA2aToken()).thenReturn("test-token");
        when(service.gradeStructured(argThat(request -> request.userId() == 7L
                && request.evidence().equals(List.of("I went home.")))))
                .thenReturn(result());

        mockMvc.perform(get("/.well-known/agent-card.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skills[0].id").value("composition-grading"));

        mockMvc.perform(post("/a2a").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer test-token").content(requestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.task.status.state").value("TASK_STATE_COMPLETED"))
                .andExpect(jsonPath("$.result.task.artifacts[0].parts[0].data.criteria[0].criterion").value("TASK_IDEAS"))
                .andExpect(jsonPath("$.result.task.artifacts[0].parts[0].data.usage.totalTokens").value(12));

        verify(service).gradeStructured(argThat(request -> request.evidence().equals(List.of("I went home."))));
    }

    @Test
    void rejectsAnUnauthenticatedA2aCall() throws Exception {
        when(properties.getA2aToken()).thenReturn("test-token");

        mockMvc.perform(post("/a2a").contentType(MediaType.APPLICATION_JSON).content(requestBody()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    void returnsARetriableServerErrorWhenGradingExecutionFails() throws Exception {
        when(properties.getA2aToken()).thenReturn("test-token");
        when(service.gradeStructured(any())).thenThrow(new IllegalStateException("model result is incomplete"));

        mockMvc.perform(post("/a2a").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer test-token").content(requestBody()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value(-32603))
                .andExpect(jsonPath("$.error.message").value("grading execution failed"));
    }

    private static A2aGradingResult result() {
        return new A2aGradingResult(new BigDecimal("72.00"), Map.of("TASK_IDEAS", new BigDecimal("72.00")),
                List.of(new A2aGradingResult.CriterionAssessment("TASK_IDEAS", new BigDecimal("72.00"),
                        "The event is clear.", List.of(new A2aGradingResult.Evidence("I went home.", "paragraph 1")))),
                0.9, "test-model", "grading-v2", new AiUsage(10, 2, 0, 12, 1,
                "OPENAI", "test-model", "test-v1", 1, 1, 1));
    }

    private static String requestBody() {
        return """
                {"jsonrpc":"2.0","id":"request-1","method":"SendMessage","params":{"message":{"contextId":"workflow-1","parts":[{"mediaType":"application/json","data":{"composition":{"userId":7,"title":"A day","content":"I went home.","language":"ENGLISH"},"evidence":{"criteria":[{"evidence":[{"excerpt":"I went home."}]}],"rubricSnapshot":{"version":"test-v1","criteria":[{"name":"TASK_IDEAS","weight":100,"guidance":"Develop ideas."}]}}}}]}}}
                """;
    }
}
