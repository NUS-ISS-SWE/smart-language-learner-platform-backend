package com.nus.iss.service;

import org.springframework.stereotype.Component;

import com.nus.iss.dto.CompositionResponse;
import com.nus.iss.dto.ModerationResponse;
import com.nus.iss.entity.Composition;
import com.nus.iss.enums.ModerationStatus;

/** Centralizes composition response mapping for command and query paths. */
@Component
public class CompositionMapper {

    public CompositionResponse toResponse(Composition composition) {
        return map(composition, false);
    }

    public CompositionResponse toOwnerResponse(Composition composition) {
        return map(composition, composition.getModerationStatus() == ModerationStatus.REMOVED);
    }

    private CompositionResponse map(Composition composition, boolean redactOwnerFields) {
        ModerationResponse moderation = new ModerationResponse(composition.getModerationStatus(),
                composition.getModerationReasonCategory(), composition.getModerationReasonNote(),
                composition.getModeratedAt(), redactOwnerFields);
        return new CompositionResponse(
                composition.getId(), composition.getUserId(), composition.getTitle(), redactOwnerFields ? null : composition.getContent(),
                composition.getCompositionSource(), composition.getLanguage(), composition.getCompositionStatus(),
                redactOwnerFields ? null : composition.getSystemFeedback(), redactOwnerFields ? null : composition.getSuggestionStatus(),
                redactOwnerFields ? null : composition.getSuggestions(), redactOwnerFields ? null : composition.getGradingProgressMessage(),
                redactOwnerFields ? null : composition.getGradingProgressStage(), composition.getStoragePath(), composition.getSourceDocumentId(), composition.getCreatedAt(),
                composition.getUpdatedAt(), moderation);
    }
}
