package com.nus.iss.service;

import com.nus.iss.dto.RubricCriterionRequest;
import com.nus.iss.dto.RubricCriterionResponse;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;

import java.util.List;

public interface RubricCriterionService {

    RubricCriterionResponse create(RubricCriterionRequest request, String username);

    List<RubricCriterionResponse> getAll(Language language, DifficultyLevel difficultyLevel);

    RubricCriterionResponse getById(Long id);

    RubricCriterionResponse update(Long id, RubricCriterionRequest request, String username);

    void delete(Long id);
}
