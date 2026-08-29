package com.nus.iss.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Prompt metadata accepted for AI-assisted composition generation. */
public record GenerateCompositionRequest(
        @NotBlank(message = "Composition generation title is required")
        @Size(max = 255, message = "Composition title must not exceed 255 characters")
        String title) {
}
