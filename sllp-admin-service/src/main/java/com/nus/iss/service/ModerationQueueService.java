package com.nus.iss.service;

import java.time.LocalDate;

import com.nus.iss.dto.ModerationQueueResponse;

public interface ModerationQueueService {
    ModerationQueueResponse list(String moderationStatus, Long userId, String compositionSource, String language,
            LocalDate from, LocalDate to, int page, int size);
}
