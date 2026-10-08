package com.nus.iss.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChallengeSubmissionRequest(
        @NotBlank(message = "Challenge response is required")
        @Size(max = 5000, message = "Challenge response must not exceed 5000 characters")
        String content) { }
