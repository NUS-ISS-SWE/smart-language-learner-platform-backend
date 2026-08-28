package com.nus.iss.service.impl;

import com.nus.iss.agent.GradingAgentRequest;
import com.nus.iss.agent.GradingAgentService;
import com.nus.iss.dto.CompositionEvaluationResult;
import com.nus.iss.dto.CompositionGradingRequestedEvent;
import com.nus.iss.enums.Language;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void delegatesValidatedRequestToGradingAgent() {
        GradingAgentService gradingAgent = mock(GradingAgentService.class);
        CompositionEvaluationServiceImpl service = new CompositionEvaluationServiceImpl(gradingAgent);
        CompositionGradingRequestedEvent event = validEvent();
        CompositionEvaluationResult expected = new CompositionEvaluationResult(
                new BigDecimal("82.50"), "Good structure");
        when(gradingAgent.grade(any())).thenReturn(expected);

        CompositionEvaluationResult actual = service.evaluate(event);

        assertEquals(expected, actual);
        ArgumentCaptor<GradingAgentRequest> requestCaptor = ArgumentCaptor.forClass(GradingAgentRequest.class);
        verify(gradingAgent).grade(requestCaptor.capture());
        GradingAgentRequest request = requestCaptor.getValue();
        assertEquals(1L, request.userId());
        assertEquals(Language.ENGLISH, request.language());
        assertEquals("My composition", request.content());
        assertEquals(0, request.evidence().size());
    }

    @Test
    void rejectsRequestWithoutUserIdBeforeCallingAgent() {
        GradingAgentService gradingAgent = mock(GradingAgentService.class);
        CompositionEvaluationServiceImpl service = new CompositionEvaluationServiceImpl(gradingAgent);
        CompositionGradingRequestedEvent event = validEvent();
        event.setUserId(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> service.evaluate(event));

        assertEquals("User ID is required", exception.getMessage());
    }

    private static CompositionGradingRequestedEvent validEvent() {
        return CompositionGradingRequestedEvent.builder()
                .compositionId(10L)
                .userId(1L)
                .title("My title")
                .content("My composition")
                .language(Language.ENGLISH)
                .build();
    }
}
