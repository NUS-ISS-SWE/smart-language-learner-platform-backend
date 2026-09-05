package com.nus.iss.service.impl;

import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.CompositionGenerationRequestedEvent;
import com.nus.iss.contracts.grading.CompositionGradingRequestedEvent;
import com.nus.iss.service.KafkaProducerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class KafkaProducerServiceImpl implements KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SllpProperties sllpProperties;
    private final ObjectMapper objectMapper;

    public KafkaProducerServiceImpl(KafkaTemplate<String, String> kafkaTemplate,
                                    SllpProperties sllpProperties,
                                    ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.sllpProperties = sllpProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void sendCompositionGradingRequestedEvent(CompositionGradingRequestedEvent event) {
        String topic = sllpProperties.getKafka().getTopics().getCompositionGradingRequested();
        sendEvent(topic, event.getCompositionId(), event, "composition grading requested");
    }

    @Override
    public void sendCompositionGenerationRequestedEvent(CompositionGenerationRequestedEvent event) {
        String topic = sllpProperties.getKafka().getTopics().getCompositionGenerationRequested();
        sendEvent(topic, event.getCompositionId(), event, "composition generation requested");
    }

    private void sendEvent(String topic, Long compositionId, Object event, String eventName) {
        String key = String.valueOf(compositionId);
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, payload);
            log.info("Sent {} event to topic: {}, compositionId: {}", eventName, topic, compositionId);
        } catch (JacksonException ex) {
            throw new IllegalStateException("Failed to serialize " + eventName + " event", ex);
        }
    }
}
