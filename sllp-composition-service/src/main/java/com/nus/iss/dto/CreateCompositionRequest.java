package com.nus.iss.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Fields accepted when a learner creates a composition. */
public record CreateCompositionRequest(
        @NotBlank(message = "Composition title is required")
        @Size(max = 255, message = "Composition title must not exceed 255 characters")
        String title,
        @NotBlank(message = "Composition content is required")
        String content,
        @Size(max = 36, message = "Source document ID must not exceed 36 characters")
        String sourceDocumentId) {
}
