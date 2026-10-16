package com.nus.iss.repository;

import com.nus.iss.model.CompositionModerationFlag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class CompositionModerationFlagRepositoryTest {
    @Autowired private CompositionModerationFlagRepository repository;

    @Test
    void persistsFlaggedModerationAudit() {
        UUID id = UUID.randomUUID();
        repository.saveAndFlush(CompositionModerationFlag.builder()
                .id(id)
                .requestId("request-1")
                .contextId("workflow-1")
                .compositionId(10L)
                .userId(7L)
                .contentSha256("0".repeat(64))
                .contentLength(31)
                .stage("COMPOSITION_INPUT")
                .decision("BLOCKED")
                .providerResponseId("moderation-1")
                .model("omni-moderation-latest")
                .providerFlagged(true)
                .matchedCategoryScores("{\"violence\":0.9}")
                .threshold(0.80)
                .createdAt(LocalDateTime.now())
                .build());

        CompositionModerationFlag saved = repository.findById(id).orElseThrow();
        assertEquals(10L, saved.getCompositionId());
        assertEquals("{\"violence\":0.9}", saved.getMatchedCategoryScores());
        assertTrue(saved.isProviderFlagged());
    }
}
