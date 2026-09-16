package com.nus.iss.controller;

import com.nus.iss.dto.EvidenceCollectionResult;
import com.nus.iss.service.EvidenceCollectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev/test trigger for now — the thing that will eventually be replaced (or joined) by a Kafka
 * listener or an A2A controller once the orchestrator's integration point is settled. Everything
 * below this just calls {@link EvidenceCollectionService}, which doesn't know or care how it
 * was triggered.
 */
@RestController
@RequestMapping("/api/v1/evidence")
public class EvidenceController {

    private final EvidenceCollectionService evidenceCollectionService;

    public EvidenceController(EvidenceCollectionService evidenceCollectionService) {
        this.evidenceCollectionService = evidenceCollectionService;
    }

    @PostMapping("/{compositionId}/collect")
    public ResponseEntity<EvidenceCollectionResult> collect(@RequestHeader("X-Sllp-User-Id") Long userId,
                                                              @PathVariable Long compositionId) {
        return ResponseEntity.ok(evidenceCollectionService.collect(userId, compositionId));
    }

    @GetMapping("/{compositionId}")
    public ResponseEntity<EvidenceCollectionResult> getLast(@PathVariable Long compositionId) {
        return evidenceCollectionService.getLast(compositionId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
