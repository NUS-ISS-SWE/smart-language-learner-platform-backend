package com.nus.iss.service.impl;

import com.nus.iss.dto.SuggestionFeedbackRequest;
import com.nus.iss.dto.SuggestionFeedbackResponse;
import com.nus.iss.entity.Composition;
import com.nus.iss.entity.SuggestionFeedback;
import com.nus.iss.enums.CompositionStatus;
import com.nus.iss.exception.InvalidCompositionRequestException;
import com.nus.iss.repository.SuggestionFeedbackRepository;
import com.nus.iss.service.CompositionAccessService;
import com.nus.iss.service.SuggestionFeedbackService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class SuggestionFeedbackServiceImpl implements SuggestionFeedbackService {
    private final CompositionAccessService access;
    private final SuggestionFeedbackRepository repository;

    @Override @Transactional
    public SuggestionFeedbackResponse upsert(Long userId, Long compositionId, int suggestionIndex, SuggestionFeedbackRequest request) {
        Composition composition = access.requireOwned(userId, compositionId);
        if (composition.getCompositionStatus() != CompositionStatus.REVIEWED || composition.getSuggestions() == null
                || suggestionIndex < 0 || suggestionIndex >= composition.getSuggestions().size()) {
            throw new InvalidCompositionRequestException("Suggestion feedback is only available for a displayed suggestion");
        }
        LocalDateTime now = LocalDateTime.now();
        SuggestionFeedback feedback = repository.findByCompositionIdAndUserIdAndSuggestionIndex(compositionId, userId, suggestionIndex)
                .orElseGet(() -> SuggestionFeedback.builder().compositionId(compositionId).userId(userId)
                        .suggestionIndex(suggestionIndex).createdAt(now).build());
        feedback.setRating(request.rating());
        feedback.setReason(request.reason());
        feedback.setUpdatedAt(now);
        repository.save(feedback);
        return new SuggestionFeedbackResponse(compositionId, suggestionIndex, feedback.getRating(), feedback.getReason(), now);
    }
}
