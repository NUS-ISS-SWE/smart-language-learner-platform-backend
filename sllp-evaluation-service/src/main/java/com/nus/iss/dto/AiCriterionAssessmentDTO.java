package com.nus.iss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCriterionAssessmentDTO {
    private String criterion;
    private Integer bandScore;
    private String rationale;
    private List<EvidenceDTO> evidence;
}
