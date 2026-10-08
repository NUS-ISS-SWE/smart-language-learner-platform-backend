package com.nus.iss.service.impl;

import com.nus.iss.dto.AiCriterionAssessmentDTO;
import com.nus.iss.dto.CriterionAssessmentDTO;
import com.nus.iss.dto.GradingScoreResult;
import com.nus.iss.dto.WorkflowRubricCriterionDTO;
import com.nus.iss.service.RubricCalculator;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RubricCalculatorImpl implements RubricCalculator {
    private static final BigDecimal BAND_PERCENT_MULTIPLIER = BigDecimal.valueOf(20);

    @Override
    public GradingScoreResult calculate(List<AiCriterionAssessmentDTO> assessments,
                                        List<WorkflowRubricCriterionDTO> rubric) {
        Map<String, AiCriterionAssessmentDTO> byName = assessments.stream()
                .collect(Collectors.toMap(AiCriterionAssessmentDTO::getCriterion, Function.identity()));
        List<CriterionAssessmentDTO> scoredCriteria = rubric.stream()
                .map(criterion -> scored(criterion, byName.get(criterion.name())))
                .toList();
        Map<String, BigDecimal> scores = new LinkedHashMap<>();
        scoredCriteria.forEach(item -> scores.put(item.getCriterion(), item.getScore()));
        Map<String, CriterionAssessmentDTO> scoredByName = scoredCriteria.stream()
                .collect(Collectors.toMap(CriterionAssessmentDTO::getCriterion, Function.identity()));
        BigDecimal overall = rubric.stream()
                .map(criterion -> scoredByName.get(criterion.name()).getScore()
                        .multiply(criterion.weight()).movePointLeft(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        log.info("Deterministic rubric score calculated criteriaCount={} overallScore={}",
                scoredCriteria.size(), overall);
        return new GradingScoreResult(overall, scores, scoredCriteria);
    }

    private static CriterionAssessmentDTO scored(WorkflowRubricCriterionDTO rubric,
                                                 AiCriterionAssessmentDTO assessment) {
        BigDecimal normalizedScore = BigDecimal.valueOf(assessment.getBandScore())
                .multiply(BAND_PERCENT_MULTIPLIER)
                .setScale(2, RoundingMode.HALF_UP);
        return CriterionAssessmentDTO.builder()
                .criterion(rubric.name())
                .bandScore(assessment.getBandScore())
                .score(normalizedScore)
                .rationale(assessment.getRationale())
                .evidence(assessment.getEvidence())
                .build();
    }
}
