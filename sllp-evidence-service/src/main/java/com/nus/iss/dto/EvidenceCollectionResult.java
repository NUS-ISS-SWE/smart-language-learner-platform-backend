package com.nus.iss.dto;

import java.util.List;

public record EvidenceCollectionResult(
        Long compositionId,
        List<String> plan,
        List<CriterionEvidence> criteria,
        List<RubricCriterionDTO> rubric,
        AiUsage usage) {

    /** Compatibility constructor for the legacy HTTP endpoint and existing callers. */
    public EvidenceCollectionResult(Long compositionId, List<String> plan, List<CriterionEvidence> criteria, AiUsage usage) {
        this(compositionId, plan, criteria, List.of(), usage);
    }
}
