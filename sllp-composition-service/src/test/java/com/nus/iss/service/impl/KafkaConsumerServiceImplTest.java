package com.nus.iss.service.impl;

import com.nus.iss.dto.CompositionGenerationCompletedEvent;
import com.nus.iss.dto.CompositionGradingCompletedEvent;
import com.nus.iss.service.CompositionCompletionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerServiceImplTest {

    @Mock
    private CompositionCompletionService completionService;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void consumeCompositionGradingCompletedEventDeserializesAndDelegatesToCompletionService() throws JacksonException {
        KafkaConsumerServiceImpl service = new KafkaConsumerServiceImpl(completionService, objectMapper);
        CompositionGradingCompletedEvent event = CompositionGradingCompletedEvent.builder()
                .compositionId(10L)
                .userId(1L)
                .success(true)
                .feedback("Good")
                .build();
        when(objectMapper.readValue("payload", CompositionGradingCompletedEvent.class)).thenReturn(event);

        service.consumeCompositionGradingCompletedEvent("payload");

        verify(completionService).completeGrading(event);
    }

    @Test
    void consumeCompositionGenerationCompletedEventDeserializesAndDelegatesToCompletionService() throws JacksonException {
        KafkaConsumerServiceImpl service = new KafkaConsumerServiceImpl(completionService, objectMapper);
        CompositionGenerationCompletedEvent event = CompositionGenerationCompletedEvent.builder()
                .compositionId(20L)
                .userId(1L)
                .success(true)
                .generatedContent("Generated")
                .build();
        when(objectMapper.readValue("payload", CompositionGenerationCompletedEvent.class)).thenReturn(event);

        service.consumeCompositionGenerationCompletedEvent("payload");

        verify(completionService).completeGeneration(event);
    }

    @Test
    void consumeCompositionGradingCompletedEvent_whenPayloadCannotDeserializeThrowsIllegalArgumentException()
            throws JacksonException {
        KafkaConsumerServiceImpl service = new KafkaConsumerServiceImpl(completionService, objectMapper);
        when(objectMapper.readValue("payload", CompositionGradingCompletedEvent.class))
                .thenThrow(Mockito.mock(JacksonException.class));

        assertThrows(IllegalArgumentException.class,
                () -> service.consumeCompositionGradingCompletedEvent("payload"));

        verifyNoInteractions(completionService);
    }

    @Test
    void consumeCompositionGenerationCompletedEvent_whenPayloadCannotDeserializeThrowsIllegalArgumentException()
            throws JacksonException {
        KafkaConsumerServiceImpl service = new KafkaConsumerServiceImpl(completionService, objectMapper);
        when(objectMapper.readValue("payload", CompositionGenerationCompletedEvent.class))
                .thenThrow(Mockito.mock(JacksonException.class));

        assertThrows(IllegalArgumentException.class,
                () -> service.consumeCompositionGenerationCompletedEvent("payload"));

        verifyNoInteractions(completionService);
    }
}
