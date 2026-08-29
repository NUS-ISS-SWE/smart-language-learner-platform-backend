package com.nus.iss.service;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nus.iss.dto.CompositionResponse;
import com.nus.iss.repository.CompositionRepository;

/** Handles read-only composition retrieval workflows. */
@Service
public class CompositionQueryService {
    private final CompositionRepository compositionRepository;
    private final CompositionAccessService compositionAccessService;
    private final CompositionMapper compositionMapper;

    public CompositionQueryService(CompositionRepository compositionRepository,
                                   CompositionAccessService compositionAccessService,
                                   CompositionMapper compositionMapper) {
        this.compositionRepository = compositionRepository;
        this.compositionAccessService = compositionAccessService;
        this.compositionMapper = compositionMapper;
    }

    @Transactional(readOnly = true)
    public List<CompositionResponse> listForUser(Long userId, Pageable pageable) {
        compositionAccessService.requireUserId(userId);
        compositionAccessService.requirePageable(pageable);
        return compositionRepository.findByUserId(userId, pageable).stream().map(compositionMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CompositionResponse getForUser(Long userId, Long compositionId) {
        return compositionMapper.toResponse(compositionAccessService.requireOwned(userId, compositionId));
    }

    @Transactional(readOnly = true)
    public CompositionResponse getForAdmin(Long compositionId) {
        return compositionMapper.toResponse(compositionAccessService.requireById(compositionId));
    }
}
