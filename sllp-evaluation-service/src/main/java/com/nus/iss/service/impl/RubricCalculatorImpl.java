package com.nus.iss.service.impl;

import com.nus.iss.config.SllpConstant;
import com.nus.iss.dto.A2aRequestDTO;
import com.nus.iss.dto.CriterionAssessmentDTO;
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
    @Override
    public Map<String, BigDecimal> rubricScores(List<CriterionAssessmentDTO> assessments) {
        Map<String, BigDecimal> scores = new LinkedHashMap<>();
        assessments.forEach(item -> scores.put(item.getCriterion(), normalized(item.getScore())));
        return scores;
    }

    @Override
    public BigDecimal overallScore(List<CriterionAssessmentDTO> assessments,
                                   A2aRequestDTO.RubricSnapshot snapshot) {
        Map<String, CriterionAssessmentDTO> byName = assessments.stream()
                .collect(Collectors.toMap(CriterionAssessmentDTO::getCriterion, Function.identity()));
        BigDecimal weighted = snapshot.getCriteria().stream()
                .map(criterion -> byName.get(criterion.getName()).getScore()
                        .multiply(criterion.getWeight()).movePointLeft(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal result = normalized(weighted);
        log.info("Deterministic rubric score calculated rubricVersion={} criteriaCount={} overallScore={}",
                snapshot.getVersion(), assessments.size(), result);
        return result;
    }

    private static BigDecimal normalized(BigDecimal score) {
        return score.max(SllpConstant.MIN_SCORE).min(SllpConstant.MAX_SCORE)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
