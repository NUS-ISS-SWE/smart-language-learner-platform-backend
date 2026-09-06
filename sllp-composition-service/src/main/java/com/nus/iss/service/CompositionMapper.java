package com.nus.iss.service;

import org.springframework.stereotype.Component;

import com.nus.iss.dto.CompositionResponse;
import com.nus.iss.entity.Composition;

/** Centralizes composition response mapping for command and query paths. */
@Component
public class CompositionMapper {

    public CompositionResponse toResponse(Composition composition) {
        return new CompositionResponse(
                composition.getId(), composition.getUserId(), composition.getTitle(), composition.getContent(),
                composition.getCompositionSource(), composition.getLanguage(), composition.getCompositionStatus(),
                composition.getSystemFeedback(), composition.getSuggestionStatus(), composition.getSuggestions(), composition.getGradingProgressMessage(), composition.getGradingProgressStage(), composition.getStoragePath(), composition.getCreatedAt(),
                composition.getUpdatedAt());
    }
}
