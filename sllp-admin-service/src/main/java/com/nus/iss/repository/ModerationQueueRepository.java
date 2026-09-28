package com.nus.iss.repository;

import java.time.LocalDateTime;

import com.nus.iss.dto.ModerationQueueResponse;

public interface ModerationQueueRepository {
    ModerationQueueResponse find(String moderationStatus, Long userId, String compositionSource, String language,
            LocalDateTime from, LocalDateTime toExclusive, int page, int size);
}
