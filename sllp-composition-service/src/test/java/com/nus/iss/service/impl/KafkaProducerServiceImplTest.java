package com.nus.iss.service.impl;

import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.CompositionGenerationRequestedEvent;
import com.nus.iss.dto.CompositionGradingRequestedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceImplTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void sendCompositionGradingRequestedEventSerializesAndSendsToConfiguredTopic() throws JacksonException {
        SllpProperties properties = sllpProperties();
        KafkaProducerServiceImpl service = new KafkaProducerServiceImpl(kafkaTemplate, properties, objectMapper);
        CompositionGradingRequestedEvent event = CompositionGradingRequestedEvent.builder()
                .compositionId(10L)
                .userId(1L)
                .build();
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"compositionId\":10}");

        service.sendCompositionGradingRequestedEvent(event);

        verify(kafkaTemplate).send("grading-requested-topic", "10", "{\"compositionId\":10}");
    }

    @Test
    void sendCompositionGenerationRequestedEventSerializesAndSendsToConfiguredTopic() throws JacksonException {
        SllpProperties properties = sllpProperties();
        KafkaProducerServiceImpl service = new KafkaProducerServiceImpl(kafkaTemplate, properties, objectMapper);
        CompositionGenerationRequestedEvent event = CompositionGenerationRequestedEvent.builder()
                .compositionId(20L)
                .userId(1L)
                .build();
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"compositionId\":20}");

        service.sendCompositionGenerationRequestedEvent(event);

        verify(kafkaTemplate).send("generation-requested-topic", "20", "{\"compositionId\":20}");
    }

    @Test
    void sendCompositionGradingRequestedEvent_whenSerializationFailsThrowsIllegalStateException() throws JacksonException {
        SllpProperties properties = sllpProperties();
        KafkaProducerServiceImpl service = new KafkaProducerServiceImpl(kafkaTemplate, properties, objectMapper);
        CompositionGradingRequestedEvent event = CompositionGradingRequestedEvent.builder()
                .compositionId(10L)
                .userId(1L)
                .build();
        when(objectMapper.writeValueAsString(event)).thenThrow(Mockito.mock(JacksonException.class));

        assertThrows(IllegalStateException.class, () -> service.sendCompositionGradingRequestedEvent(event));

        verify(kafkaTemplate, never()).send(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    private static SllpProperties sllpProperties() {
        SllpProperties properties = new SllpProperties();
        SllpProperties.Topics topics = new SllpProperties.Topics();
        topics.setCompositionGradingRequested("grading-requested-topic");
        topics.setCompositionGenerationRequested("generation-requested-topic");
        properties.getKafka().setTopics(topics);
        return properties;
    }
}
