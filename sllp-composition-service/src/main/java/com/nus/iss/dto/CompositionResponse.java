package com.nus.iss.dto;

import java.time.LocalDateTime;

import com.nus.iss.enums.CompositionSource;
import com.nus.iss.enums.CompositionStatus;
import com.nus.iss.enums.Language;
import com.nus.iss.contracts.grading.ImprovementSuggestion;

import java.util.List;

/** Immutable representation returned by the composition API. */
public record CompositionResponse(
        Long id,
        Long userId,
        String title,
        String content,
        CompositionSource compositionSource,
        Language language,
        CompositionStatus compositionStatus,
        String systemFeedback,
        String suggestionStatus,
        List<ImprovementSuggestion> suggestions,
        String gradingProgressMessage,
        String gradingProgressStage,
        String storagePath,
        String sourceDocumentId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        ModerationResponse moderation) {

    /** Compatibility constructor for existing command and controller tests. */
    public CompositionResponse(Long id, Long userId, String title, String content,
            CompositionSource compositionSource, Language language, CompositionStatus compositionStatus,
            String systemFeedback, String suggestionStatus, List<ImprovementSuggestion> suggestions,
            String gradingProgressMessage, String gradingProgressStage, String storagePath,
            String sourceDocumentId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, userId, title, content, compositionSource, language, compositionStatus, systemFeedback,
                suggestionStatus, suggestions, gradingProgressMessage, gradingProgressStage, storagePath,
                sourceDocumentId, createdAt, updatedAt, null);
    }
}
