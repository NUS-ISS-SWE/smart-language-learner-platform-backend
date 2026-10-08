package com.nus.iss.service;

import com.nus.iss.dto.AiCriterionAssessmentDTO;
import com.nus.iss.dto.GradingScoreResult;
import com.nus.iss.dto.WorkflowRubricCriterionDTO;

import java.util.List;

public interface RubricCalculator {
    GradingScoreResult calculate(List<AiCriterionAssessmentDTO> assessments,
                                 List<WorkflowRubricCriterionDTO> rubric);
}
