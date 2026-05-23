package com.nus.iss.service.impl;

import com.nus.iss.dto.CompositionGradingCompletedEvent;
import com.nus.iss.dto.CompositionGenerationCompletedEvent;
import com.nus.iss.service.CompositionService;
import com.nus.iss.service.KafkaConsumerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
public class KafkaConsumerServiceImpl implements KafkaConsumerService {

    private final CompositionService compositionService;
    private final ObjectMapper objectMapper;

    public KafkaConsumerServiceImpl(CompositionService compositionService, ObjectMapper objectMapper) {
        this.compositionService = compositionService;
        this.objectMapper = objectMapper;
    }

    @Override
    @KafkaListener(
            topics = "${sllp.kafka.topics.composition-grading-completed}",
            groupId = "${spring.application.name}"
    )
    public void consumeCompositionGradingCompletedEvent(String payload) {
        try {
            CompositionGradingCompletedEvent event = objectMapper.readValue(payload, CompositionGradingCompletedEvent.class);
            compositionService.completeCompositionGrading(event);
            log.info("Consumed composition grading completed event for compositionId: {}", event.getCompositionId());
        } catch (JacksonException ex) {
            throw new IllegalArgumentException("Failed to deserialize composition grading completed event", ex);
        }
    }

    @Override
    @KafkaListener(
            topics = "${sllp.kafka.topics.composition-generation-completed}",
            groupId = "${spring.application.name}"
    )
    public void consumeCompositionGenerationCompletedEvent(String payload) {
        try {
            CompositionGenerationCompletedEvent event = objectMapper.readValue(payload, CompositionGenerationCompletedEvent.class);
            compositionService.completeCompositionGeneration(event);
            log.info("Consumed composition generation completed event for compositionId: {}", event.getCompositionId());
        } catch (JacksonException ex) {
            throw new IllegalArgumentException("Failed to deserialize composition generation completed event", ex);
        }
    }
}
