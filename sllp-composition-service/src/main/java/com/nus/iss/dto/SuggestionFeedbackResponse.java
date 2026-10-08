package com.nus.iss.dto;

import com.nus.iss.enums.SuggestionFeedbackRating;
import com.nus.iss.enums.SuggestionFeedbackReason;
import java.time.LocalDateTime;

public record SuggestionFeedbackResponse(Long compositionId, int suggestionIndex, SuggestionFeedbackRating rating,
                                         SuggestionFeedbackReason reason, LocalDateTime updatedAt) {}
