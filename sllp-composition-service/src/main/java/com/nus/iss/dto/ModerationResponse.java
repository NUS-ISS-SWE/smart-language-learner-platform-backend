package com.nus.iss.dto;

import java.time.LocalDateTime;

import com.nus.iss.enums.ModerationReasonCategory;
import com.nus.iss.enums.ModerationStatus;

/** Current moderation decision included with a composition response. */
public record ModerationResponse(
        ModerationStatus status,
        ModerationReasonCategory reasonCategory,
        String reasonNote,
        LocalDateTime moderatedAt,
        boolean ownerRedacted) {
}
