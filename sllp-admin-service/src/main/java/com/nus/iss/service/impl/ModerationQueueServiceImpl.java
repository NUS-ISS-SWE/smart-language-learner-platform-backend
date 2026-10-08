package com.nus.iss.service.impl;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nus.iss.dto.ModerationQueueResponse;
import com.nus.iss.repository.ModerationQueueRepository;
import com.nus.iss.service.ModerationQueueService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModerationQueueServiceImpl implements ModerationQueueService {
    private final ModerationQueueRepository repository;

    @Override
    @Transactional(readOnly = true)
    public ModerationQueueResponse list(String moderationStatus, Long userId, String compositionSource, String language,
            LocalDate from, LocalDate to, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Page size must be between 1 and 100.");
        if (userId != null && userId < 1) throw new IllegalArgumentException("User ID must be positive.");
        if (from != null && to != null && to.isBefore(from)) throw new IllegalArgumentException("The end date must not be before the start date.");
        return repository.find(normalize(moderationStatus), userId, normalize(compositionSource), normalize(language),
                from == null ? null : from.atStartOfDay(), to == null ? null : to.plusDays(1).atStartOfDay(), page, size);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }
}
