package com.nus.iss.service.impl;

import com.nus.iss.agent.EvidenceAgentRequest;
import com.nus.iss.agent.EvidenceAgentService;
import com.nus.iss.client.CompositionClient;
import com.nus.iss.dto.CompositionDTO;
import com.nus.iss.dto.EvidenceCollectionResult;
import com.nus.iss.exception.EvidenceGenerationException;
import com.nus.iss.memory.EvidenceMemoryStore;
import com.nus.iss.service.EvidenceCollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * The one entry point the trigger mechanism calls. Kept deliberately transport-agnostic: today
 * {@link com.nus.iss.controller.EvidenceController} calls it over REST; later, a Kafka listener
 * or an A2A controller can call this exact same method without this class changing at all.
 */
@Service
public class EvidenceCollectionServiceImpl implements EvidenceCollectionService {

    private static final Logger log = LoggerFactory.getLogger(EvidenceCollectionServiceImpl.class);

    private final CompositionClient compositionClient;
    private final EvidenceAgentService evidenceAgentService;
    private final EvidenceMemoryStore memoryStore;

    public EvidenceCollectionServiceImpl(CompositionClient compositionClient,
                                          EvidenceAgentService evidenceAgentService,
                                          EvidenceMemoryStore memoryStore) {
        this.compositionClient = compositionClient;
        this.evidenceAgentService = evidenceAgentService;
        this.memoryStore = memoryStore;
    }

    @Override
    public EvidenceCollectionResult collect(Long userId, Long compositionId) {
        String requestId = UUID.randomUUID().toString();

        log.info("requestId={} compositionId={} action=fetch_composition", requestId, compositionId);
        CompositionDTO composition = compositionClient.getComposition(userId, compositionId);

        if (composition.content() == null || composition.content().isBlank()) {
            throw new EvidenceGenerationException("Composition " + compositionId + " has no content to search for evidence");
        }

        log.info("requestId={} compositionId={} action=collect_evidence", requestId, compositionId);
        EvidenceAgentRequest request = new EvidenceAgentRequest(
                compositionId, userId, composition.title(), composition.content(), composition.language());
        EvidenceCollectionResult result = evidenceAgentService.collect(request, requestId);

        memoryStore.put(compositionId, result);
        log.info("requestId={} compositionId={} action=done criteriaCount={}",
                requestId, compositionId, result.criteria().size());
        return result;
    }

    @Override
    public Optional<EvidenceCollectionResult> getLast(Long compositionId) {
        return memoryStore.get(compositionId);
    }
}
