package com.nus.iss.service;

import com.nus.iss.dto.SuggestionFeedbackRequest;
import com.nus.iss.dto.SuggestionFeedbackResponse;

public interface SuggestionFeedbackService {
    SuggestionFeedbackResponse upsert(Long userId, Long compositionId, int suggestionIndex, SuggestionFeedbackRequest request);
}
