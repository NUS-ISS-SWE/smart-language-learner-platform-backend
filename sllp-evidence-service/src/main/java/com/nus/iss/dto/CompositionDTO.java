package com.nus.iss.dto;

import com.nus.iss.enums.Language;

/** Subset of composition-service's CompositionResponse this service actually needs. */
public record CompositionDTO(
        Long id,
        Long userId,
        String title,
        String content,
        Language language) {
}
