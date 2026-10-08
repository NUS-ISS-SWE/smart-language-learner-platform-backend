package com.nus.iss.dto;

import java.math.BigDecimal;

/** Immutable rubric criterion carried in the orchestrator's workflow snapshot. */
public record WorkflowRubricCriterionDTO(
        String name,
        BigDecimal weight,
        String guidance) {
}
