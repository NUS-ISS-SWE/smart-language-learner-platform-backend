package com.nus.iss.service.impl;

import com.nus.iss.dto.AdminRubricCriterionDTO;
import com.nus.iss.dto.AiCriterionAssessmentDTO;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GradingCalibrationRegressionTest {
    private static final List<String> CRITERIA = List.of(
            "TASK_IDEAS", "ORGANISATION_COHESION", "GRAMMAR_SENTENCE_CONTROL",
            "VOCABULARY_WORD_CHOICE", "MECHANICS", "EXPRESSION_DESCRIPTION_VOICE");
    private static final List<BigDecimal> WEIGHTS = List.of(
            new BigDecimal("25"), new BigDecimal("20"), new BigDecimal("20"),
            new BigDecimal("15"), new BigDecimal("10"), new BigDecimal("10"));

    @ParameterizedTest(name = "{0} remains within its manually calibrated score range")
    @MethodSource("benchmarks")
    void fixedAnchorCompositionRemainsWithinAcceptedRange(Benchmark benchmark) {
        var result = new RubricCalculatorImpl().calculate(
                assessments(benchmark.bands()), rubric(benchmark.difficulty()));

        assertTrue(result.overallScore().compareTo(benchmark.minimum()) >= 0
                        && result.overallScore().compareTo(benchmark.maximum()) <= 0,
                () -> "%s (%s) scored %s outside %s-%s".formatted(
                        benchmark.title(), benchmark.composition(), result.overallScore(),
                        benchmark.minimum(), benchmark.maximum()));
    }

    private static Stream<Benchmark> benchmarks() {
        return Stream.of(
                new Benchmark("My Surprise Birthday", "A surprise party with developed actions and feelings.",
                        DifficultyLevel.BEGINNER, List.of(5, 4, 4, 4, 5, 4),
                        new BigDecimal("85"), new BigDecimal("90")),
                new Benchmark("Our Class Garden", "A class plans, grows and reflects on a shared garden.",
                        DifficultyLevel.INTERMEDIATE, List.of(3, 3, 3, 3, 4, 3),
                        new BigDecimal("60"), new BigDecimal("65")),
                new Benchmark("The Choice I Regretted", "A pupil avoids project work, lies and later repairs the mistake.",
                        DifficultyLevel.ADVANCED, List.of(1, 2, 2, 2, 1, 1),
                        new BigDecimal("30"), new BigDecimal("35"))
        );
    }

    private static List<AdminRubricCriterionDTO> rubric(DifficultyLevel difficulty) {
        return IntStream.range(0, CRITERIA.size())
                .mapToObj(index -> new AdminRubricCriterionDTO(
                        (long) index + 1, Language.ENGLISH, difficulty, CRITERIA.get(index),
                        "Benchmark criterion", WEIGHTS.get(index), "SYSTEM", "SYSTEM", null, null))
                .toList();
    }

    private static List<AiCriterionAssessmentDTO> assessments(List<Integer> bands) {
        return IntStream.range(0, CRITERIA.size())
                .mapToObj(index -> AiCriterionAssessmentDTO.builder()
                        .criterion(CRITERIA.get(index))
                        .bandScore(bands.get(index))
                        .rationale("Manually calibrated anchor score.")
                        .evidence(List.of())
                        .build())
                .toList();
    }

    private record Benchmark(String title, String composition, DifficultyLevel difficulty,
                             List<Integer> bands, BigDecimal minimum, BigDecimal maximum) {
        @Override
        public String toString() {
            return title + " / " + difficulty;
        }
    }
}
