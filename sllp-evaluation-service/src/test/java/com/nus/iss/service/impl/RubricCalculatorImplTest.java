package com.nus.iss.service.impl;

import com.nus.iss.dto.WorkflowRubricCriterionDTO;
import com.nus.iss.dto.AiCriterionAssessmentDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RubricCalculatorImplTest {
    private final RubricCalculatorImpl calculator = new RubricCalculatorImpl();

    @Test
    void convertsBandsAndDerivesWeightedPercentage() {
        List<AiCriterionAssessmentDTO> assessments = List.of(
                AiCriterionAssessmentDTO.builder().criterion("Ideas").bandScore(4).build(),
                AiCriterionAssessmentDTO.builder().criterion("Grammar").bandScore(3).build());
        List<WorkflowRubricCriterionDTO> rubric = List.of(
                criterion("Ideas", "60"),
                criterion("Grammar", "40"));

        var result = calculator.calculate(assessments, rubric);

        assertEquals(new BigDecimal("72.00"), result.overallScore());
        assertEquals(new BigDecimal("80.00"), result.rubricScores().get("Ideas"));
        assertEquals(4, result.criteria().getFirst().getBandScore());
        assertEquals(new BigDecimal("80.00"), result.criteria().getFirst().getScore());
    }

    private static WorkflowRubricCriterionDTO criterion(String name, String weight) {
        return new WorkflowRubricCriterionDTO(name, new BigDecimal(weight), name + " guidance");
    }
}
