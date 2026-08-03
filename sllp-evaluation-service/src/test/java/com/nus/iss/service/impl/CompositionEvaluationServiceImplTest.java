package com.nus.iss.service.impl;

import com.nus.iss.dto.CompositionGradingRequestedEvent;
import com.nus.iss.enums.DifficultyLevel;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

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
}
