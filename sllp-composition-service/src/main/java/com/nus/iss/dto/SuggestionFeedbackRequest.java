package com.nus.iss.dto;

import com.nus.iss.enums.SuggestionFeedbackRating;
import com.nus.iss.enums.SuggestionFeedbackReason;
import jakarta.validation.constraints.NotNull;

public record SuggestionFeedbackRequest(@NotNull SuggestionFeedbackRating rating, SuggestionFeedbackReason reason) {}
