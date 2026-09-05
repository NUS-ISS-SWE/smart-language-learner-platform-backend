package com.nus.iss.agent;

import com.nus.iss.contracts.grading.CompositionGradingRequestedEvent;
import com.nus.iss.enums.Language;

import java.util.List;

public record GradingAgentRequest(
        Long userId,
        String title,
        String content,
        Language language,
        List<String> evidence
) {

    public GradingAgentRequest {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public static GradingAgentRequest from(CompositionGradingRequestedEvent event) {
        return new GradingAgentRequest(
                event.getUserId(),
                event.getTitle(),
                event.getContent(),
                Language.valueOf(event.getLanguage()),
                List.of());
    }
}
