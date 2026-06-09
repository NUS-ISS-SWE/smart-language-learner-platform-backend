package com.nus.iss.service;

import com.nus.iss.dto.CompositionEvaluationResult;
import com.nus.iss.dto.CompositionGradingRequestedEvent;

public interface CompositionEvaluationService {
    CompositionEvaluationResult evaluate(CompositionGradingRequestedEvent event);
}
