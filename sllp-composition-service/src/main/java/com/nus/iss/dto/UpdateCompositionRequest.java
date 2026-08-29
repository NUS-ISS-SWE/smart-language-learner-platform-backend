package com.nus.iss.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Fields a learner may change on an existing draft. */
public record UpdateCompositionRequest(
        @NotBlank(message = "Composition title is required")
        @Size(max = 255, message = "Composition title must not exceed 255 characters")
        String title,
        @NotBlank(message = "Composition content is required")
        String content) {
}
