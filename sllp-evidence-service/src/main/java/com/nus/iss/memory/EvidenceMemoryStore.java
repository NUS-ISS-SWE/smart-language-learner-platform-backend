package com.nus.iss.memory;

import com.nus.iss.dto.EvidenceCollectionResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cross-run memory: what evidence did we find for this composition last time. In-memory on
 * purpose — a demo-scale store for a single-instance service, not a durability guarantee.
 */
@Component
public class EvidenceMemoryStore {

    private final Map<Long, EvidenceCollectionResult> store = new ConcurrentHashMap<>();

    public void put(Long compositionId, EvidenceCollectionResult result) {
        store.put(compositionId, result);
    }

    public Optional<EvidenceCollectionResult> get(Long compositionId) {
        return Optional.ofNullable(store.get(compositionId));
    }
}
