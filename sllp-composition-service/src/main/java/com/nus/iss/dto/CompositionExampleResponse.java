package com.nus.iss.dto;

import com.nus.iss.enums.Language;

/** Immutable example composition returned by the examples API. */
public record CompositionExampleResponse(
        String title,
        String content,
        Language language,
        String proficiencyLevel,
        String description) {
}
