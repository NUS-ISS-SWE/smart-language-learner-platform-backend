package com.nus.iss.service.impl;

import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.CompositionGradingCompletedEvent;
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
    public void sendCompositionGradingCompletedEvent(CompositionGradingCompletedEvent event) {
        try {
            String topic = sllpProperties.getKafka().getTopics().getCompositionGradingCompleted();
            String key = String.valueOf(event.getCompositionId());
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, payload);
            log.info("Sent composition grading completed event to topic: {}, compositionId: {}, success: {}",
                    topic, event.getCompositionId(), event.isSuccess());
        } catch (JacksonException ex) {
            throw new IllegalStateException("Failed to serialize composition grading completed event", ex);
        }
    }
}
