package com.nus.iss.service;

import com.nus.iss.dto.ChallengeResponse;
import com.nus.iss.dto.ChallengeSubmissionRequest;
import com.nus.iss.contracts.grading.CompositionGradingCompletedEvent;
import com.nus.iss.entity.ChallengeTemplate;
import com.nus.iss.entity.LearningGoal;
import com.nus.iss.entity.AchievementDefinition;
import com.nus.iss.entity.AssessmentLearningEvent;
import com.nus.iss.enums.ChallengeCadence;
import com.nus.iss.enums.GoalMetric;
import com.nus.iss.enums.GoalStatus;
import com.nus.iss.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class LearningProgressServiceTest {
    @Mock private ChallengeTemplateRepository templates;
    @Mock private ChallengeSubmissionRepository submissions;
    @Mock private LearningGoalRepository goals;
    @Mock private AchievementDefinitionRepository definitions;
    @Mock private AchievementAwardRepository awards;
    @Mock private AssessmentLearningEventRepository assessmentEvents;
    @Mock private LearningActivityDayRepository activityDays;

    @Test
    void resolvesTheOnlyActiveTemplateAndMarksTheCurrentCycleSubmitted() {
        ChallengeTemplate template = ChallengeTemplate.builder().id(42L).code("DAILY_EN_TEST")
                .cadence(ChallengeCadence.DAILY).language("ENGLISH").title("A small win")
                .prompt("Write something.").minResponseUnits(35).maxResponseUnits(80).active(true).displayOrder(1).build();
        when(templates.findByCadenceAndLanguageAndActiveTrueOrderByDisplayOrderAscIdAsc(ChallengeCadence.DAILY, "ENGLISH"))
                .thenReturn(List.of(template));
        when(submissions.findByUserIdAndCadenceAndCycleKey(eq(7L), eq(ChallengeCadence.DAILY), anyString()))
                .thenReturn(Optional.empty());

        LearningProgressService service = new LearningProgressService(templates, submissions, goals, definitions, awards, assessmentEvents, activityDays);

        ChallengeResponse response = service.currentChallenge(7L, ChallengeCadence.DAILY, "english");

        assertEquals(42L, response.id());
        assertEquals("ENGLISH", response.language());
        assertEquals(35, response.minResponseUnits());
        assertFalse(response.submitted());
        assertTrue(response.cycleKey().startsWith("DAILY:"));
    }

    @Test
    void reportsNoChallengeInsteadOfFallingBackToAnotherLanguage() {
        when(templates.findByCadenceAndLanguageAndActiveTrueOrderByDisplayOrderAscIdAsc(ChallengeCadence.WEEKLY, "CHINESE"))
                .thenReturn(List.of());
        LearningProgressService service = new LearningProgressService(templates, submissions, goals, definitions, awards, assessmentEvents, activityDays);

        NoSuchElementException error = assertThrows(NoSuchElementException.class,
                () -> service.currentChallenge(7L, ChallengeCadence.WEEKLY, "CHINESE"));

        assertTrue(error.getMessage().contains("No active weekly challenge"));
    }

    @Test
    void completesAnOverallScoreGoalFromTheFinalAssessmentEvent() {
        LearningGoal goal = LearningGoal.builder().id(9L).userId(7L).metric(GoalMetric.OVERALL_ASSESSMENT_SCORE)
                .target(BigDecimal.valueOf(80)).progress(BigDecimal.ZERO).status(GoalStatus.ACTIVE)
                .startsOn(LocalDate.now()).createdAt(LocalDateTime.now().minusMinutes(1)).build();
        when(assessmentEvents.existsById(any())).thenReturn(false);
        when(goals.findByUserIdAndStatus(7L, GoalStatus.ACTIVE)).thenReturn(List.of(goal));
        when(definitions.findByActiveTrue()).thenReturn(List.of());
        LearningProgressService service = new LearningProgressService(templates, submissions, goals, definitions, awards, assessmentEvents, activityDays);

        service.recordAssessment(CompositionGradingCompletedEvent.builder().requestId(UUID.randomUUID()).userId(7L)
                .compositionId(10L).success(true).score(BigDecimal.valueOf(82)).completedAt(LocalDateTime.now()).build());

        assertEquals(GoalStatus.COMPLETED, goal.getStatus());
        assertEquals(0, BigDecimal.valueOf(82).compareTo(goal.getProgress()));
        verify(assessmentEvents).save(any());
    }

    @Test
    void completesVocabularyGoalFromFinalAssessmentRubricSnapshot() {
        LearningGoal goal = LearningGoal.builder().id(10L).userId(7L).metric(GoalMetric.VOCABULARY_RUBRIC_SCORE)
                .target(BigDecimal.valueOf(75)).progress(BigDecimal.ZERO).status(GoalStatus.ACTIVE)
                .startsOn(LocalDate.now()).createdAt(LocalDateTime.now().minusMinutes(1)).build();
        when(assessmentEvents.existsById(any())).thenReturn(false);
        when(goals.findByUserIdAndStatus(7L, GoalStatus.ACTIVE)).thenReturn(List.of(goal));
        when(definitions.findByActiveTrue()).thenReturn(List.of());
        LearningProgressService service = new LearningProgressService(templates, submissions, goals, definitions, awards, assessmentEvents, activityDays);

        service.recordAssessment(CompositionGradingCompletedEvent.builder().requestId(UUID.randomUUID()).userId(7L)
                .compositionId(10L).success(true).score(BigDecimal.valueOf(82)).rubricScores(Map.of("VOCABULARY_WORD_CHOICE", BigDecimal.valueOf(78)))
                .completedAt(LocalDateTime.now()).build());

        assertEquals(GoalStatus.COMPLETED, goal.getStatus());
        assertEquals(0, BigDecimal.valueOf(78).compareTo(goal.getProgress()));
        verify(activityDays).save(any());
    }

    @Test
    void completesRichTextWeeklyChallengeWithoutUsingAnOutOfRangeTimestamp() {
        ChallengeTemplate template = ChallengeTemplate.builder().id(42L).code("WEEKLY_EN_TEST")
                .cadence(ChallengeCadence.WEEKLY).language("ENGLISH").title("Weekly")
                .prompt("Write.").minResponseUnits(2).maxResponseUnits(100).active(true).displayOrder(1).build();
        AchievementDefinition weeklyBadge = AchievementDefinition.builder().id(5L).code("FIRST_WEEKLY")
                .title("Weekly").description("Complete one.").category("WEEKLY_CHALLENGES").threshold(1).active(true).build();
        when(templates.findByCadenceAndLanguageAndActiveTrueOrderByDisplayOrderAscIdAsc(ChallengeCadence.WEEKLY, "ENGLISH"))
                .thenReturn(List.of(template));
        when(templates.findById(42L)).thenReturn(Optional.of(template));
        when(submissions.findByUserIdAndCadenceAndCycleKey(eq(7L), eq(ChallengeCadence.WEEKLY), anyString()))
                .thenReturn(Optional.empty());
        when(submissions.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(goals.findByUserIdAndStatus(7L, GoalStatus.ACTIVE)).thenReturn(List.of());
        when(definitions.findByActiveTrue()).thenReturn(List.of(weeklyBadge));
        when(submissions.countByUserId(7L)).thenReturn(1L);
        when(submissions.countByUserIdAndCadence(7L, ChallengeCadence.WEEKLY)).thenReturn(1L);
        LearningProgressService service = new LearningProgressService(templates, submissions, goals, definitions, awards, assessmentEvents, activityDays);

        service.submit(7L, 42L, new ChallengeSubmissionRequest("<p>one <strong>two</strong></p>"), "ENGLISH");

        verify(submissions).countByUserIdAndCadence(7L, ChallengeCadence.WEEKLY);
        verify(submissions, never()).countByUserIdAndCadenceAndSubmittedAtGreaterThanEqual(eq(7L), eq(ChallengeCadence.WEEKLY), any());
    }

    @Test
    void awardsPersonalBestBadgeOnlyWhenTheAssessmentSetsANewBest() {
        AchievementDefinition badge = AchievementDefinition.builder().id(12L).code("PERSONAL_BEST_80")
                .title("Personal best").description("New best.").category("PERSONAL_BEST_SCORE").threshold(80).active(true).build();
        when(assessmentEvents.existsById(any())).thenReturn(false);
        when(assessmentEvents.findFirstByUserIdAndScoreIsNotNullOrderByScoreDesc(7L)).thenReturn(Optional.of(
                AssessmentLearningEvent.builder().userId(7L).score(BigDecimal.valueOf(79)).completedAt(LocalDateTime.now()).build()));
        when(goals.findByUserIdAndStatus(7L, GoalStatus.ACTIVE)).thenReturn(List.of());
        when(definitions.findByActiveTrue()).thenReturn(List.of(badge));
        when(awards.existsByUserIdAndAchievementDefinitionId(7L, 12L)).thenReturn(false);
        LearningProgressService service = new LearningProgressService(templates, submissions, goals, definitions, awards, assessmentEvents, activityDays);

        service.recordAssessment(CompositionGradingCompletedEvent.builder().requestId(UUID.randomUUID()).userId(7L)
                .compositionId(10L).success(true).score(BigDecimal.valueOf(82)).completedAt(LocalDateTime.now()).build());

        verify(awards).save(any());
    }
}
