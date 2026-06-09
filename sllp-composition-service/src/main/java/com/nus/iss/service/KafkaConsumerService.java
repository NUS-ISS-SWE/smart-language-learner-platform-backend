package com.nus.iss.service;

public interface KafkaConsumerService {

    void consumeCompositionGradingCompletedEvent(String payload);

    void consumeCompositionGenerationCompletedEvent(String payload);
}
