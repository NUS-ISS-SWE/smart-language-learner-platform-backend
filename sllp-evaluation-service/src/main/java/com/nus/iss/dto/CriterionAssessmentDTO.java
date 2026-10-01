package com.nus.iss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriterionAssessmentDTO {
    private Long rubricCriterionId;
    private String criterion;
    private Integer bandScore;
    private BigDecimal score;
    private String rationale;
    private List<EvidenceDTO> evidence;
}
