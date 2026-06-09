package com.nus.iss.service;

import com.nus.iss.dto.CompositionGradingRequestedEvent;
import com.nus.iss.dto.CompositionGenerationRequestedEvent;

public interface KafkaProducerService {
    void sendCompositionGradingRequestedEvent(CompositionGradingRequestedEvent event);

    void sendCompositionGenerationRequestedEvent(CompositionGenerationRequestedEvent event);
}
