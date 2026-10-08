package com.nus.iss.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradingResultDTO {
    @JsonIgnore
    private String taskId;
    @JsonIgnore
    private String artifactId;
    private BigDecimal overallScore;
    private Map<String, BigDecimal> rubricScores;
    private String rubricVersion;
    private List<CriterionAssessmentDTO> criteria;
    private double confidence;
    private String modelVersion;
    private String promptVersion;
    private AiUsage usage;
}
