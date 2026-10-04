package com.nus.iss.service;

import com.nus.iss.dto.A2aRequestDTO;
import com.nus.iss.dto.AiGradingOutputDTO;
import com.nus.iss.dto.GradingRequestDTO;
import com.nus.iss.dto.WorkflowRubricCriterionDTO;

import java.util.List;

public interface ValidationService {
    void validateAuthorization(String authorization);

    GradingRequestDTO validateAndExtract(A2aRequestDTO request);

    void validateRubric(List<WorkflowRubricCriterionDTO> rubric);

    void validateAiOutput(AiGradingOutputDTO output, GradingRequestDTO request, List<WorkflowRubricCriterionDTO> rubric);
}
