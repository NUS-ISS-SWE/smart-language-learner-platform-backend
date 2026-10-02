package com.nus.iss.service;

import com.nus.iss.dto.A2aRequestDTO;
import com.nus.iss.dto.AdminRubricCriterionDTO;
import com.nus.iss.dto.AiGradingOutputDTO;
import com.nus.iss.dto.GradingRequestDTO;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;

import java.util.List;

public interface ValidationService {
    void validateAuthorization(String authorization);

    GradingRequestDTO validateAndExtract(A2aRequestDTO request);

    void validateRubric(List<AdminRubricCriterionDTO> rubric, Language language, DifficultyLevel difficultyLevel);

    void validateAiOutput(AiGradingOutputDTO output, GradingRequestDTO request, List<AdminRubricCriterionDTO> rubric);
}
