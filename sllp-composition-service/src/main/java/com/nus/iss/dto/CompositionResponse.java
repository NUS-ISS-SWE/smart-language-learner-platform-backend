package com.nus.iss.dto;

import java.time.LocalDateTime;

import com.nus.iss.enums.CompositionSource;
import com.nus.iss.enums.CompositionStatus;
import com.nus.iss.enums.Language;

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
        String gradingProgressMessage,
        String storagePath,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
