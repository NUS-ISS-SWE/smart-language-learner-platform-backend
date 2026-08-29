package com.nus.iss.service;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.nus.iss.entity.Composition;
import com.nus.iss.exception.CompositionNotFoundException;
import com.nus.iss.exception.InvalidCompositionRequestException;
import com.nus.iss.repository.CompositionRepository;

/** Owns common identifier validation and composition lookups. */
@Service
public class CompositionAccessService {
    private final CompositionRepository compositionRepository;

    public CompositionAccessService(CompositionRepository compositionRepository) {
        this.compositionRepository = compositionRepository;
    }

    public Composition requireOwned(Long userId, Long compositionId) {
        requireUserId(userId);
        requireCompositionId(compositionId);
        return compositionRepository.findByIdAndUserId(compositionId, userId)
                .orElseThrow(CompositionNotFoundException::new);
    }

    public Composition requireById(Long compositionId) {
        requireCompositionId(compositionId);
        return compositionRepository.findById(compositionId).orElseThrow(CompositionNotFoundException::new);
    }

    public void requireUserId(Long userId) {
        if (userId == null || userId < 1) throw new InvalidCompositionRequestException("Valid user ID header is required");
    }

    public void requireCompositionId(Long compositionId) {
        if (compositionId == null || compositionId < 1) throw new InvalidCompositionRequestException("Composition ID is required");
    }

    public void requirePageable(Pageable pageable) {
        if (pageable == null) throw new InvalidCompositionRequestException("Pagination information is required");
        if (pageable.getPageNumber() < 0) throw new InvalidCompositionRequestException("Page number must not be negative");
        if (pageable.getPageSize() < 1 || pageable.getPageSize() > 100) {
            throw new InvalidCompositionRequestException("Page size must be between 1 and 100");
        }
    }
}
