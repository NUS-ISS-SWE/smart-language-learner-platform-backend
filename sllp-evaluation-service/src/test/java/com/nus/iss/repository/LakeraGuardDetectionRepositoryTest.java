package com.nus.iss.repository;

import com.nus.iss.model.LakeraGuardDetection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DataJpaTest
class LakeraGuardDetectionRepositoryTest {
    @Autowired private LakeraGuardDetectionRepository repository;

    @Test
    void persistsLakeraDetectionAudit() {
        UUID id = UUID.randomUUID();
        repository.saveAndFlush(LakeraGuardDetection.builder()
                .id(id)
                .requestId("request-1")
                .contextId("workflow-1")
                .compositionId(10L)
                .userId(7L)
                .screenedText("Title: A day\n\nComposition:\nText")
                .providerRequestUuid("guard-request-1")
                .action("detect")
                .providerFlagged(false)
                .detectorBreakdown("[{\"detected\":true}]")
                .providerResponse("{\"flagged\":false}")
                .createdAt(LocalDateTime.now())
                .build());

        LakeraGuardDetection saved = repository.findById(id).orElseThrow();
        assertEquals("guard-request-1", saved.getProviderRequestUuid());
        assertEquals("Title: A day\n\nComposition:\nText", saved.getScreenedText());
        assertEquals("[{\"detected\":true}]", saved.getDetectorBreakdown());
        assertFalse(saved.isProviderFlagged());
    }
}
