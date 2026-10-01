package com.nus.iss.service;

import com.nus.iss.dto.AdminRubricCriterionDTO;
import com.nus.iss.dto.AiCriterionAssessmentDTO;
import com.nus.iss.dto.GradingScoreResult;

import java.util.List;

public interface RubricCalculator {
    GradingScoreResult calculate(List<AiCriterionAssessmentDTO> assessments,
                                 List<AdminRubricCriterionDTO> rubric);
}
