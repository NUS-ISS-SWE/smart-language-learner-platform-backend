package com.nus.iss.dto;

import java.time.LocalDateTime;

/** Lightweight cross-service read model for the administrator moderation queue. */
public record ModerationQueueItem(
        long compositionId,
        long userId,
        String username,
        String title,
        String compositionSource,
        String language,
        String compositionStatus,
        String moderationStatus,
        String reasonCategory,
        String reasonNote,
        LocalDateTime createdAt,
        LocalDateTime moderatedAt) {
}
