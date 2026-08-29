package com.nus.iss.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nus.iss.dto.CompositionExampleResponse;
import com.nus.iss.enums.Language;

class CompositionExampleServiceImplTest {

    private final CompositionExampleServiceImpl service = new CompositionExampleServiceImpl();

    @Test
    void getAllExamplesReturnsMultipleExamples() {
        List<CompositionExampleResponse> examples = service.getAllExamples();

        assertTrue(examples.size() > 0, "Should return at least one example");
    }

    @Test
    void getAllExamplesContainsMultipleLanguages() {
        List<CompositionExampleResponse> examples = service.getAllExamples();

        boolean hasEnglish = examples.stream().anyMatch(e -> e.language() == Language.ENGLISH);
        boolean hasChinese = examples.stream().anyMatch(e -> e.language() == Language.CHINESE);

        assertTrue(hasEnglish, "Should have English examples");
        assertTrue(hasChinese, "Should have Chinese examples");
    }

    @Test
    void getExamplesByLanguageFiltersCorrectly() {
        List<CompositionExampleResponse> examples = service.getExamples(Language.ENGLISH);

        assertTrue(examples.size() > 0, "Should return English examples");
        assertTrue(examples.stream().allMatch(e -> e.language() == Language.ENGLISH));
    }

    @Test
    void getExamplesByLevelFiltersCorrectly() {
        List<CompositionExampleResponse> beginners = service.getExamplesByLevel(Language.ENGLISH, "BEGINNER");
        List<CompositionExampleResponse> intermediate = service.getExamplesByLevel(Language.ENGLISH, "INTERMEDIATE");
        List<CompositionExampleResponse> advanced = service.getExamplesByLevel(Language.ENGLISH, "ADVANCED");

        assertTrue(beginners.size() > 0, "Should have beginner examples");
        assertTrue(intermediate.size() > 0, "Should have intermediate examples");
        assertTrue(advanced.size() > 0, "Should have advanced examples");
        assertTrue(beginners.stream().allMatch(e -> "BEGINNER".equals(e.proficiencyLevel())));
        assertTrue(intermediate.stream().allMatch(e -> "INTERMEDIATE".equals(e.proficiencyLevel())));
        assertTrue(advanced.stream().allMatch(e -> "ADVANCED".equals(e.proficiencyLevel())));
    }

    @Test
    void examplesHaveRequiredFields() {
        List<CompositionExampleResponse> examples = service.getAllExamples();

        for (CompositionExampleResponse example : examples) {
            assertTrue(example.title() != null && !example.title().isBlank(), "Title should not be blank");
            assertTrue(example.content() != null && !example.content().isBlank(), "Content should not be blank");
            assertTrue(example.language() != null, "Language should not be null");
            assertTrue(example.proficiencyLevel() != null && !example.proficiencyLevel().isBlank(),
                    "Proficiency level should not be blank");
            assertTrue(example.description() != null && !example.description().isBlank(),
                    "Description should not be blank");
        }
    }
}
