package com.nus.iss.service;

public interface KafkaConsumerService {
    void consumeCompositionGradingRequestedEvent(String payload);
}
