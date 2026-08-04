package com.nus.iss.service.impl;

import com.nus.iss.dto.RubricCriterionRequest;
import com.nus.iss.dto.RubricCriterionResponse;
import com.nus.iss.entity.RubricCriterion;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import com.nus.iss.exception.RubricCriterionNotFoundException;
import com.nus.iss.repository.RubricCriterionRepository;
import com.nus.iss.service.RubricCriterionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RubricCriterionServiceImpl implements RubricCriterionService {

    private static final int USERNAME_MAX_LENGTH = 100;
    private static final Sort DEFAULT_SORT = Sort.by(
            Sort.Order.asc("language"),
            Sort.Order.asc("difficultyLevel"),
            Sort.Order.asc("id"));

    private final RubricCriterionRepository repository;

    @Override
    @Transactional
    public RubricCriterionResponse create(RubricCriterionRequest request, String username) {
        String auditUsername = normalizeUsername(username);
        RubricCriterion criterion = RubricCriterion.builder()
                .language(request.language())
                .difficultyLevel(request.difficultyLevel())
                .criterionName(request.criterionName().trim())
                .criterionDescription(request.criterionDescription().trim())
                .weightPercentage(request.weightPercentage())
                .createdBy(auditUsername)
                .updatedBy(auditUsername)
                .build();

        return toResponse(repository.save(criterion));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RubricCriterionResponse> getAll(Language language, DifficultyLevel difficultyLevel) {
        List<RubricCriterion> criteria;
        if (language != null && difficultyLevel != null) {
            criteria = repository.findByLanguageAndDifficultyLevel(language, difficultyLevel, DEFAULT_SORT);
        } else if (language != null) {
            criteria = repository.findByLanguage(language, DEFAULT_SORT);
        } else if (difficultyLevel != null) {
            criteria = repository.findByDifficultyLevel(difficultyLevel, DEFAULT_SORT);
        } else {
            criteria = repository.findAll(DEFAULT_SORT);
        }

        return criteria.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RubricCriterionResponse getById(Long id) {
        return toResponse(findRequired(id));
    }

    @Override
    @Transactional
    public RubricCriterionResponse update(Long id, RubricCriterionRequest request, String username) {
        RubricCriterion criterion = findRequired(id);
        criterion.setLanguage(request.language());
        criterion.setDifficultyLevel(request.difficultyLevel());
        criterion.setCriterionName(request.criterionName().trim());
        criterion.setCriterionDescription(request.criterionDescription().trim());
        criterion.setWeightPercentage(request.weightPercentage());
        criterion.setUpdatedBy(normalizeUsername(username));

        return toResponse(repository.save(criterion));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        repository.delete(findRequired(id));
    }

    private RubricCriterion findRequired(Long id) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("Rubric criterion ID must be a positive number");
        }
        return repository.findById(id).orElseThrow(() -> new RubricCriterionNotFoundException(id));
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Valid username header is required");
        }
        String normalizedUsername = username.trim();
        if (normalizedUsername.length() > USERNAME_MAX_LENGTH) {
            throw new IllegalArgumentException("Username must not exceed 100 characters");
        }
        return normalizedUsername;
    }

    private RubricCriterionResponse toResponse(RubricCriterion criterion) {
        return new RubricCriterionResponse(
                criterion.getId(),
                criterion.getLanguage(),
                criterion.getDifficultyLevel(),
                criterion.getCriterionName(),
                criterion.getCriterionDescription(),
                criterion.getWeightPercentage(),
                criterion.getCreatedBy(),
                criterion.getUpdatedBy(),
                criterion.getCreatedAt(),
                criterion.getUpdatedAt());
    }
}
