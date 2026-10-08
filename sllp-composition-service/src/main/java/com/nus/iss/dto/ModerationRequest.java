package com.nus.iss.dto;

import com.nus.iss.enums.ModerationAction;
import com.nus.iss.enums.ModerationReasonCategory;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModerationRequest(
        @NotNull ModerationAction action,
        ModerationReasonCategory reasonCategory,
        @Size(max = 500) String reasonNote) {
}
