package com.nus.iss.dto;

import java.util.List;

public record EvidenceCollectionResult(
        Long compositionId,
        List<String> plan,
        List<CriterionEvidence> criteria,
        AiUsage usage) {
}
