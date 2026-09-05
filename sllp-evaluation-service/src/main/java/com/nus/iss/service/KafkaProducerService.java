package com.nus.iss.service;

import com.nus.iss.contracts.grading.CompositionGradingCompletedEvent;

public interface KafkaProducerService {
    void sendCompositionGradingCompletedEvent(CompositionGradingCompletedEvent event);
}
