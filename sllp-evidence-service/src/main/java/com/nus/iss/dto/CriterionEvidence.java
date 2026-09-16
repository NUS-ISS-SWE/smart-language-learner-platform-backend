package com.nus.iss.dto;

import java.util.List;

public record CriterionEvidence(
        String criterion,
        List<EvidenceItem> evidence) {
}
