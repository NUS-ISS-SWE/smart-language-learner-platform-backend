package com.nus.iss.service.impl;

import com.nus.iss.dto.AdminRubricCriterionDTO;
import com.nus.iss.dto.AiCriterionAssessmentDTO;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
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
        List<AdminRubricCriterionDTO> rubric = List.of(
                criterion(11L, "Ideas", "60"),
                criterion(12L, "Grammar", "40"));

        var result = calculator.calculate(assessments, rubric);

        assertEquals(new BigDecimal("72.00"), result.overallScore());
        assertEquals(new BigDecimal("80.00"), result.rubricScores().get("Ideas"));
        assertEquals(11L, result.criteria().getFirst().getRubricCriterionId());
        assertEquals(4, result.criteria().getFirst().getBandScore());
        assertEquals(new BigDecimal("80.00"), result.criteria().getFirst().getScore());
    }

    private static AdminRubricCriterionDTO criterion(Long id, String name, String weight) {
        return new AdminRubricCriterionDTO(id, Language.ENGLISH, DifficultyLevel.INTERMEDIATE,
                name, name + " guidance", new BigDecimal(weight), "SYSTEM", "SYSTEM", null, null);
    }
}
