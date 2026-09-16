package com.nus.iss.service;

import com.nus.iss.dto.EvidenceCollectionResult;

import java.util.Optional;

public interface EvidenceCollectionService {
    EvidenceCollectionResult collect(Long userId, Long compositionId);

    Optional<EvidenceCollectionResult> getLast(Long compositionId);
}
