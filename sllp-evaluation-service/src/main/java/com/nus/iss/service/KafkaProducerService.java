package com.nus.iss.service;

import com.nus.iss.dto.CompositionGradingCompletedEvent;

public interface KafkaProducerService {
    void sendCompositionGradingCompletedEvent(CompositionGradingCompletedEvent event);
}
