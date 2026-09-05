package com.nus.iss.service.impl;

import com.nus.iss.agent.GradingAgentRequest;
import com.nus.iss.agent.GradingAgentService;
import com.nus.iss.dto.CompositionEvaluationResult;
import com.nus.iss.contracts.grading.CompositionGradingRequestedEvent;
import com.nus.iss.enums.Language;
import org.springframework.stereotype.Service;

@Service
public class CompositionEvaluationServiceImpl implements com.nus.iss.service.CompositionEvaluationService {

    private final GradingAgentService gradingAgentService;

    public CompositionEvaluationServiceImpl(GradingAgentService gradingAgentService) {
        this.gradingAgentService = gradingAgentService;
    }

    @Override
    public CompositionEvaluationResult evaluate(CompositionGradingRequestedEvent event) {
        validateEvent(event);
        return gradingAgentService.grade(GradingAgentRequest.from(event));
    }

    private void validateEvent(CompositionGradingRequestedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Composition grading request is required");
        }
        if (event.getCompositionId() == null || event.getCompositionId() < 1) {
            throw new IllegalArgumentException("Composition ID is required");
        }
        if (event.getUserId() == null || event.getUserId() < 1) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (event.getTitle() == null || event.getTitle().isBlank()) {
            throw new IllegalArgumentException("Composition title is required");
        }
        if (event.getContent() == null || event.getContent().isBlank()) {
            throw new IllegalArgumentException("Composition content is required");
        }
        if (event.getLanguage() == null) {
            throw new IllegalArgumentException("Composition language is required");
        }
        try {
            Language.valueOf(event.getLanguage());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Composition language is invalid", exception);
        }
    }
}
