package com.nus.iss.client;

import com.nus.iss.dto.RubricCriterionDTO;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;

import java.util.List;

public interface RubricCriterionClient {
    List<RubricCriterionDTO> getRubricCriteria(Language language, DifficultyLevel difficultyLevel);
}
