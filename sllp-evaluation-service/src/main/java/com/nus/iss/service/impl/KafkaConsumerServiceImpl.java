package com.nus.iss.service.impl;

import com.nus.iss.dto.CompositionEvaluationResult;
import com.nus.iss.dto.CompositionGradingCompletedEvent;
import com.nus.iss.dto.CompositionGradingRequestedEvent;
import com.nus.iss.service.CompositionEvaluationService;
import com.nus.iss.service.KafkaConsumerService;
import com.nus.iss.service.KafkaProducerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import com.nus.iss.dto.AiUsage;

@Slf4j
@Service
public class KafkaConsumerServiceImpl implements KafkaConsumerService {

    private final CompositionEvaluationService compositionEvaluationService;
    private final KafkaProducerService kafkaProducerService;
    private final ObjectMapper objectMapper;

    public KafkaConsumerServiceImpl(CompositionEvaluationService compositionEvaluationService,
                                    KafkaProducerService kafkaProducerService,
                                    ObjectMapper objectMapper) {
        this.compositionEvaluationService = compositionEvaluationService;
        this.kafkaProducerService = kafkaProducerService;
        this.objectMapper = objectMapper;
    }

    @Override
    @KafkaListener(
            topics = "${sllp.kafka.topics.composition-grading-requested}",
            groupId = "${sllp.kafka.group-id}"
    )
    public void consumeCompositionGradingRequestedEvent(String payload) {
        log.info("CompositionGradingRequest: {}", payload);
        CompositionGradingRequestedEvent request = deserializeRequest(payload);
        try {
            CompositionEvaluationResult result = compositionEvaluationService.evaluate(request);
            CompositionGradingCompletedEvent.CompositionGradingCompletedEventBuilder event = CompositionGradingCompletedEvent.builder()
                    .requestId(request.getRequestId()).requestedAt(request.getRequestedAt())
                    .compositionId(request.getCompositionId())
                    .userId(request.getUserId())
                    .score(result.score())
                    .feedback(result.feedback())
                    .success(true)
                    .completedAt(LocalDateTime.now());
            addUsage(event, result.usage());
            kafkaProducerService.sendCompositionGradingCompletedEvent(event.build());
            log.info("Completed composition grading for compositionId: {}", request.getCompositionId());
        } catch (RuntimeException ex) {
            log.error("Failed to grade compositionId: {}", request.getCompositionId(), ex);
            kafkaProducerService.sendCompositionGradingCompletedEvent(CompositionGradingCompletedEvent.builder()
                    .requestId(request.getRequestId()).requestedAt(request.getRequestedAt())
                    .compositionId(request.getCompositionId())
                    .userId(request.getUserId())
                    .success(false)
                    .failureReason(failureReason(ex))
                    .completedAt(LocalDateTime.now())
                    .build());
        }
    }

    private static void addUsage(CompositionGradingCompletedEvent.CompositionGradingCompletedEventBuilder event, AiUsage usage) {
        if (usage == null) return;
        event.promptTokens(usage.promptTokens()).completionTokens(usage.completionTokens())
                .cachedInputTokens(usage.cachedInputTokens()).totalTokens(usage.totalTokens())
                .estimatedCostMicros(usage.estimatedCostMicros()).aiProvider(usage.provider()).aiModel(usage.model())
                .pricingVersion(usage.pricingVersion()).inputPriceMicrosPerMillion(usage.inputPriceMicrosPerMillion())
                .cachedInputPriceMicrosPerMillion(usage.cachedInputPriceMicrosPerMillion())
                .outputPriceMicrosPerMillion(usage.outputPriceMicrosPerMillion());
    }

    private String failureReason(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return "Composition grading failed";
        }
        return message;
    }

    private CompositionGradingRequestedEvent deserializeRequest(String payload) {
        try {
            return objectMapper.readValue(payload, CompositionGradingRequestedEvent.class);
        } catch (JacksonException ex) {
            throw new IllegalArgumentException("Failed to deserialize composition grading requested event", ex);
        }
    }
}
