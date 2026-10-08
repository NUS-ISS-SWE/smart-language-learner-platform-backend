package com.nus.iss.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nus.iss.dto.CompositionResponse;
import com.nus.iss.dto.ModerationRequest;
import com.nus.iss.entity.Composition;
import com.nus.iss.entity.CompositionModerationDecision;
import com.nus.iss.enums.ModerationAction;
import com.nus.iss.enums.ModerationStatus;
import com.nus.iss.exception.InvalidCompositionRequestException;
import com.nus.iss.exception.ModerationConflictException;
import com.nus.iss.repository.CompositionModerationDecisionRepository;
import com.nus.iss.repository.CompositionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompositionModerationService {
    private final CompositionRepository compositionRepository;
    private final CompositionModerationDecisionRepository decisions;
    private final CompositionAccessService accessService;
    private final CompositionMapper compositionMapper;

    @Transactional
    public CompositionResponse moderate(Long compositionId, Long moderatorId, String moderatorName,
            ModerationRequest request) {
        accessService.requireCompositionId(compositionId);
        accessService.requireUserId(moderatorId);
        if (request == null || request.action() == null) {
            throw new InvalidCompositionRequestException("A moderation action is required");
        }
        Composition composition = accessService.requireById(compositionId);
        LocalDateTime now = LocalDateTime.now();
        String normalizedModeratorName = blankToNull(moderatorName);
        String normalizedReasonNote = normalizeNote(request.reasonNote());

        if (request.action() == ModerationAction.REMOVE) {
            if (request.reasonCategory() == null) {
                throw new InvalidCompositionRequestException("A removal reason category is required");
            }
            if (composition.getModerationStatus() == ModerationStatus.REMOVED) {
                throw new ModerationConflictException("This composition is already removed");
            }
            composition.setModerationStatus(ModerationStatus.REMOVED);
            composition.setModerationReasonCategory(request.reasonCategory());
            composition.setModerationReasonNote(normalizedReasonNote);
        } else {
            if (request.reasonCategory() != null || (request.reasonNote() != null && !request.reasonNote().isBlank())) {
                throw new InvalidCompositionRequestException("A restore action must not include a removal reason");
            }
            if (composition.getModerationStatus() != ModerationStatus.REMOVED) {
                throw new ModerationConflictException("Only removed compositions can be restored");
            }
            composition.setModerationStatus(ModerationStatus.ACTIVE);
            composition.setModerationReasonCategory(null);
            composition.setModerationReasonNote(null);
        }

        composition.setModeratedById(moderatorId);
        composition.setModeratedByName(normalizedModeratorName);
        composition.setModeratedAt(now);
        Composition saved = compositionRepository.save(composition);
        decisions.save(CompositionModerationDecision.builder()
                .id(UUID.randomUUID())
                .compositionId(saved.getId())
                .action(request.action())
                .reasonCategory(request.reasonCategory())
                .reasonNote(normalizedReasonNote)
                .moderatorId(moderatorId)
                .moderatorName(normalizedModeratorName)
                .createdAt(now)
                .build());
        return compositionMapper.toResponse(saved);
    }

    private static String normalizeNote(String note) {
        String normalized = blankToNull(note);
        return normalized == null ? null : normalized.length() > 500 ? normalized.substring(0, 500) : normalized;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
