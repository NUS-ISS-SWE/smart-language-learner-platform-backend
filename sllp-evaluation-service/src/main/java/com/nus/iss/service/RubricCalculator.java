package com.nus.iss.service;

import com.nus.iss.dto.A2aRequestDTO;
import com.nus.iss.dto.CriterionAssessmentDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface RubricCalculator {
    Map<String, BigDecimal> rubricScores(List<CriterionAssessmentDTO> assessments);
    BigDecimal overallScore(List<CriterionAssessmentDTO> assessments,
                            A2aRequestDTO.RubricSnapshot rubricSnapshot);
}
