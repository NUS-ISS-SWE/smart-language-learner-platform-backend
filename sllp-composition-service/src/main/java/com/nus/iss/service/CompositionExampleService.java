package com.nus.iss.service;

import java.util.List;

import com.nus.iss.dto.CompositionExampleResponse;
import com.nus.iss.enums.Language;

/** Provides curated examples of good compositions at different proficiency levels. */
public interface CompositionExampleService {
    /**
     * Get examples for a specific language.
     *
     * @param language the language for examples
     * @return list of example compositions
     */
    List<CompositionExampleResponse> getExamples(Language language);

    /**
     * Get examples at a specific proficiency level.
     *
     * @param language the language for examples
     * @param proficiencyLevel BEGINNER, INTERMEDIATE, or ADVANCED
     * @return list of example compositions
     */
    List<CompositionExampleResponse> getExamplesByLevel(Language language, String proficiencyLevel);

    /**
     * Get all available examples.
     *
     * @return all example compositions
     */
    List<CompositionExampleResponse> getAllExamples();
}
