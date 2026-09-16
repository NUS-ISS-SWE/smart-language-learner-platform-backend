package com.nus.iss.memory;

import com.nus.iss.dto.EvidenceCollectionResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvidenceMemoryStoreTest {

    @Test
    void returnsEmptyWhenNothingStoredYet() {
        EvidenceMemoryStore store = new EvidenceMemoryStore();

        assertTrue(store.get(1L).isEmpty());
    }

    @Test
    void returnsTheLastResultStoredForAComposition() {
        EvidenceMemoryStore store = new EvidenceMemoryStore();
        EvidenceCollectionResult first = new EvidenceCollectionResult(1L, List.of("first plan"), List.of(), null);
        EvidenceCollectionResult second = new EvidenceCollectionResult(1L, List.of("second plan"), List.of(), null);

        store.put(1L, first);
        store.put(1L, second);

        Optional<EvidenceCollectionResult> result = store.get(1L);
        assertTrue(result.isPresent());
        assertEquals(List.of("second plan"), result.get().plan());
    }
}
