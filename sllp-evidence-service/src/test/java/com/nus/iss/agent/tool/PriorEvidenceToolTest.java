package com.nus.iss.agent.tool;

import com.nus.iss.dto.CriterionEvidence;
import com.nus.iss.dto.EvidenceCollectionResult;
import com.nus.iss.dto.EvidenceItem;
import com.nus.iss.enums.EvidencePolarity;
import com.nus.iss.enums.Language;
import com.nus.iss.memory.EvidenceMemoryStore;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriorEvidenceToolTest {

    private static ToolContext contextFor(Long compositionId) {
        return new ToolContext(Map.of(EvidenceToolContext.CONTEXT_KEY,
                new EvidenceToolContext(7L, compositionId, Language.ENGLISH)));
    }

    @Test
    void reportsWhenNoPriorRunExists() {
        PriorEvidenceTool tool = new PriorEvidenceTool(new EvidenceMemoryStore());

        assertEquals("No prior evidence was found for this composition.", tool.getPriorEvidence(contextFor(1L)));
    }

    @Test
    void returnsThePriorFindingsPerCriterion() {
        EvidenceMemoryStore store = new EvidenceMemoryStore();
        store.put(1L, new EvidenceCollectionResult(1L, List.of("Read for ideas", "Check language"), List.of(
                new CriterionEvidence("TASK_IDEAS", List.of(
                        new EvidenceItem("The waves sparkled.", "sentence 1", EvidencePolarity.STRENGTH, "Vivid detail.", true))),
                new CriterionEvidence("LANGUAGE", List.of(
                        new EvidenceItem("We goes home.", "sentence 2", EvidencePolarity.WEAKNESS, "Subject-verb agreement.", true)))),
                null));

        String summary = new PriorEvidenceTool(store).getPriorEvidence(contextFor(1L));

        assertTrue(summary.contains("Read for ideas; Check language"));
        assertTrue(summary.contains("- TASK_IDEAS\n  [STRENGTH] \"The waves sparkled.\" — Vivid detail."));
        assertTrue(summary.contains("- LANGUAGE\n  [WEAKNESS] \"We goes home.\" — Subject-verb agreement."));
    }
}
