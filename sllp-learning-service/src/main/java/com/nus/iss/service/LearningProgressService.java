package com.nus.iss.service;

import com.nus.iss.dto.*;
import com.nus.iss.contracts.grading.CompositionGradingCompletedEvent;
import com.nus.iss.entity.*;
import com.nus.iss.enums.*;
import com.nus.iss.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class LearningProgressService {
    public static final ZoneId PLATFORM_ZONE = ZoneId.of("Asia/Singapore");
    private static final LocalDate CYCLE_ANCHOR = LocalDate.of(2026, 1, 5);
    private static final Set<String> CHALLENGE_CATEGORIES = Set.of("REFLECTION", "DESCRIPTION", "STORYTELLING", "PRACTICAL");
    private static final Set<String> THEME_PATHS = Set.of("EVERYDAY_CONFIDENCE", "VIVID_DESCRIPTION", "STORY_BUILDER", "HELPFUL_COMMUNICATION");
    private static final Set<String> ACHIEVEMENT_CATEGORIES = Set.of("CHALLENGE_COMPLETIONS", "STREAK_DAYS", "WEEKLY_CHALLENGES", "REVIEWED_COMPOSITIONS", "OVERALL_SCORE", "VOCABULARY_SCORE", "PERSONAL_BEST_SCORE");

    private final ChallengeTemplateRepository templates;
    private final ChallengeSubmissionRepository submissions;
    private final LearningGoalRepository goals;
    private final AchievementDefinitionRepository definitions;
    private final AchievementAwardRepository awards;
    private final AssessmentLearningEventRepository assessmentEvents;
    private final LearningActivityDayRepository activityDays;

    public LearningProgressService(ChallengeTemplateRepository templates, ChallengeSubmissionRepository submissions,
            LearningGoalRepository goals, AchievementDefinitionRepository definitions, AchievementAwardRepository awards,
            AssessmentLearningEventRepository assessmentEvents, LearningActivityDayRepository activityDays) {
        this.templates = templates; this.submissions = submissions; this.goals = goals;
        this.definitions = definitions; this.awards = awards; this.assessmentEvents = assessmentEvents; this.activityDays = activityDays;
    }

    @Transactional
    public ChallengeSubmissionResponse submit(Long userId, Long challengeId, ChallengeSubmissionRequest request, String language) {
        ChallengeResponse current = currentChallenge(userId, challengeId, language);
        int units = responseUnits(visibleText(request.content()), current.language());
        if (units < current.minResponseUnits() || units > current.maxResponseUnits()) {
            throw new IllegalArgumentException("Challenge response must contain between " + current.minResponseUnits() + " and " + current.maxResponseUnits() + unitLabel(current.language()));
        }
        if (current.submitted()) throw new IllegalStateException("This challenge has already been completed for the current cycle");
        LocalDate today = LocalDate.now(PLATFORM_ZONE);
        ChallengeTemplate template = templates.findById(challengeId).orElseThrow(() -> new NoSuchElementException("Challenge not found"));
        ChallengeSubmission saved;
        try {
            saved = submissions.saveAndFlush(ChallengeSubmission.builder().userId(userId).challengeTemplateId(template.getId())
                    .cadence(template.getCadence()).cycleKey(current.cycleKey()).activityDate(today).content(request.content().trim())
                    .titleSnapshot(template.getTitle()).promptSnapshot(template.getPrompt()).categorySnapshot(template.getCategory())
                    .submittedAt(LocalDateTime.now(Clock.systemUTC())).build());
        } catch (DataIntegrityViolationException error) {
            throw new IllegalStateException("This challenge has already been completed for the current cycle");
        }
        recordActivityDay(userId, today);
        refreshCountGoals(userId, saved.getSubmittedAt());
        refreshStreakGoals(userId, saved.getSubmittedAt());
        evaluateChallengeAchievements(userId);
        return toSubmission(saved);
    }

    @Transactional
    public GoalResponse createGoal(Long userId, CreateGoalRequest request) {
        if (request.dueOn() != null && request.dueOn().isBefore(LocalDate.now(PLATFORM_ZONE))) throw new IllegalArgumentException("Goal due date cannot be in the past");
        if (isScoreGoal(request.metric()) && request.target().compareTo(BigDecimal.valueOf(100)) > 0) throw new IllegalArgumentException("Score goals cannot exceed 100");
        String category = normalizeCategory(request.category());
        if (request.metric() == GoalMetric.CATEGORY_CHALLENGE_COMPLETIONS && category == null) throw new IllegalArgumentException("A category is required for a category challenge goal");
        if (request.metric() != GoalMetric.CATEGORY_CHALLENGE_COMPLETIONS && category != null) throw new IllegalArgumentException("A category is only supported for category challenge goals");
        Optional<LearningGoal> focusedGoal = goals.findFirstByUserIdAndFocusedTrue(userId);
        focusedGoal.filter(item -> item.getStatus() != GoalStatus.ACTIVE).ifPresent(item -> item.setFocused(false));
        boolean hasFocusedActiveGoal = focusedGoal.filter(item -> item.getStatus() == GoalStatus.ACTIVE).isPresent();
        LearningGoal goal = goals.save(LearningGoal.builder().userId(userId).metric(request.metric()).category(category).target(request.target()).focused(!hasFocusedActiveGoal)
                .startsOn(LocalDate.now(PLATFORM_ZONE)).dueOn(request.dueOn()).createdAt(LocalDateTime.now(Clock.systemUTC())).build());
        return toGoal(goal);
    }

    @Transactional
    public GoalResponse cancelGoal(Long userId, Long id) {
        LearningGoal goal = goals.findById(id).filter(item -> item.getUserId().equals(userId)).orElseThrow(() -> new NoSuchElementException("Goal not found"));
        if (goal.getStatus() != GoalStatus.ACTIVE) throw new IllegalStateException("Only active goals can be cancelled");
        goal.setStatus(GoalStatus.CANCELLED);
        goal.setFocused(false);
        return toGoal(goal);
    }

    @Transactional
    public GoalResponse focusGoal(Long userId, Long id) {
        LearningGoal goal = goals.findById(id).filter(item -> item.getUserId().equals(userId)).orElseThrow(() -> new NoSuchElementException("Goal not found"));
        if (goal.getStatus() != GoalStatus.ACTIVE) throw new IllegalStateException("Only active goals can be focused");
        goals.findFirstByUserIdAndFocusedTrue(userId).filter(item -> !item.getId().equals(id)).ifPresent(item -> item.setFocused(false));
        goal.setFocused(true);
        return toGoal(goal);
    }

    public ChallengeSubmissionResponse currentSubmission(Long userId, Long challengeId, String language) {
        ChallengeResponse current = currentChallenge(userId, challengeId, language);
        return submissions.findByUserIdAndCadenceAndCycleKey(userId, current.cadence(), current.cycleKey()).map(this::toSubmission)
                .orElseThrow(() -> new NoSuchElementException("No completed entry for this challenge"));
    }

    @Transactional
    public List<GoalResponse> listGoals(Long userId) {
        expireGoals(userId);
        return goals.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toGoal).toList();
    }

    public ChallengeResponse currentChallenge(Long userId, ChallengeCadence cadence, String language) { return currentChallenge(userId, cadence, language, null); }

    public ChallengeResponse currentChallenge(Long userId, ChallengeCadence cadence, String language, String themePath) {
        LocalDate today = LocalDate.now(PLATFORM_ZONE);
        String normalizedLanguage = normalizeLanguage(language);
        List<ChallengeTemplate> candidates = templates.findByCadenceAndLanguageAndActiveTrueOrderByDisplayOrderAscIdAsc(cadence, normalizedLanguage);
        String normalizedThemePath = themePath == null || themePath.isBlank() ? null : normalizeThemePath(themePath, null);
        if (normalizedThemePath != null) candidates = candidates.stream().filter(item -> normalizedThemePath.equals(item.getThemePath())).toList();
        if (candidates.isEmpty()) throw new NoSuchElementException("No active " + cadence.name().toLowerCase() + " challenge is available for this path and language");
        LocalDate cycleStart = cadence == ChallengeCadence.DAILY ? today : today.minusDays(today.getDayOfWeek().getValue() - 1L);
        long offset = cadence == ChallengeCadence.DAILY ? ChronoUnit.DAYS.between(CYCLE_ANCHOR, cycleStart) : ChronoUnit.WEEKS.between(CYCLE_ANCHOR, cycleStart);
        ChallengeTemplate template = candidates.get(Math.floorMod(offset, candidates.size()));
        String cycleKey = cadence.name() + ":" + cycleStart;
        boolean submitted = submissions.findByUserIdAndCadenceAndCycleKey(userId, cadence, cycleKey).isPresent();
        return toChallenge(template, cycleKey, submitted);
    }

    private ChallengeResponse currentChallenge(Long userId, Long expectedId, String language) {
        ChallengeTemplate expected = templates.findById(expectedId).orElseThrow(() -> new NoSuchElementException("Challenge not found"));
        ChallengeResponse current = currentChallenge(userId, expected.getCadence(), language, expected.getThemePath());
        if (!current.id().equals(expectedId)) throw new IllegalArgumentException("This is not the active challenge for the current cycle");
        return current;
    }

    @Transactional
    public LearningSummaryResponse summary(Long userId, String language) {
        expireGoals(userId);
        List<LocalDate> dates = activityDays.findActivityDates(userId);
        int current = currentStreak(dates);
        return new LearningSummaryResponse(current, longestStreak(dates), submissions.countByUserId(userId),
                goals.findByUserIdAndStatus(userId, GoalStatus.ACTIVE).stream().map(this::toGoal).toList(),
                listAchievements(userId).stream().filter(item -> item.seenAt() == null).toList(),
                currentChallenge(userId, ChallengeCadence.DAILY, language), currentChallenge(userId, ChallengeCadence.WEEKLY, language));
    }

    public List<AchievementResponse> listAchievements(Long userId) {
        List<AchievementAward> userAwards = awards.findByUserIdOrderByAwardedAtDesc(userId);
        Map<Long, AchievementDefinition> definitionById = definitions.findAllById(
                userAwards.stream().map(AchievementAward::getAchievementDefinitionId).toList())
                .stream().collect(java.util.stream.Collectors.toMap(AchievementDefinition::getId, item -> item));
        return userAwards.stream().map(award -> toAchievement(award, definitionById.get(award.getAchievementDefinitionId())))
                .filter(Objects::nonNull).toList();
    }

    /** Records only a terminal, successful assessment once, keyed by its workflow request id. */
    @Transactional
    public void recordAssessment(CompositionGradingCompletedEvent event) {
        if (event == null || !event.isSuccess() || event.getRequestId() == null || event.getUserId() == null || event.getCompositionId() == null) return;
        if (assessmentEvents.existsById(event.getRequestId())) return;
        LocalDateTime completedAt = event.getCompletedAt() == null ? LocalDateTime.now(Clock.systemUTC()) : event.getCompletedAt();
        BigDecimal previousBest = assessmentEvents.findFirstByUserIdAndScoreIsNotNullOrderByScoreDesc(event.getUserId())
                .map(AssessmentLearningEvent::getScore).orElse(null);
        assessmentEvents.save(AssessmentLearningEvent.builder().requestId(event.getRequestId()).userId(event.getUserId())
                .compositionId(event.getCompositionId()).score(event.getScore()).completedAt(completedAt).build());
        recordActivityDay(event.getUserId(), completedAt.atZone(ZoneOffset.UTC).withZoneSameInstant(PLATFORM_ZONE).toLocalDate());
        for (LearningGoal goal : goals.findByUserIdAndStatus(event.getUserId(), GoalStatus.ACTIVE)) {
            if (goal.getDueOn() != null && goal.getDueOn().isBefore(LocalDate.now(PLATFORM_ZONE))) { goal.setStatus(GoalStatus.EXPIRED); continue; }
            if (goal.getMetric() == GoalMetric.REVIEWED_COMPOSITIONS) {
                long count = assessmentEvents.countByUserIdAndCompletedAtGreaterThanEqual(event.getUserId(), goal.getCreatedAt());
                goal.setProgress(BigDecimal.valueOf(count));
            } else if (goal.getMetric() == GoalMetric.OVERALL_ASSESSMENT_SCORE && event.getScore() != null) {
                goal.setProgress(goal.getProgress().max(event.getScore()));
            } else if (goal.getMetric() == GoalMetric.VOCABULARY_RUBRIC_SCORE && event.getRubricScores() != null
                    && event.getRubricScores().get("VOCABULARY_WORD_CHOICE") != null) {
                goal.setProgress(goal.getProgress().max(event.getRubricScores().get("VOCABULARY_WORD_CHOICE")));
            } else {
                continue;
            }
            completeGoalIfReached(goal, completedAt);
        }
        refreshStreakGoals(event.getUserId(), completedAt);
        boolean personalBest = event.getScore() != null && (previousBest == null || event.getScore().compareTo(previousBest) > 0);
        evaluateAssessmentAchievements(event.getUserId(), event.getScore(), event.getRubricScores(), personalBest);
    }

    public List<ChallengeTemplateResponse> listTemplates() {
        return templates.findAll(org.springframework.data.domain.Sort.by("cadence", "language", "displayOrder", "id"))
                .stream().map(this::toTemplate).toList();
    }

    public List<AchievementDefinitionResponse> listAchievementDefinitions() {
        return definitions.findAll(org.springframework.data.domain.Sort.by("category", "threshold", "id")).stream().map(this::toDefinition).toList();
    }

    @Transactional
    public AchievementDefinitionResponse createAchievementDefinition(AchievementDefinitionRequest request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (definitions.existsByCode(code)) throw new IllegalArgumentException("Achievement code is already in use");
        AchievementDefinition definition = AchievementDefinition.builder().code(code).title(request.title().trim())
                .description(request.description().trim()).category(normalizeAchievementCategory(request.category()))
                .threshold(request.threshold()).active(request.active()).build();
        return toDefinition(definitions.save(definition));
    }

    @Transactional
    public AchievementDefinitionResponse updateAchievementDefinition(Long id, AchievementDefinitionRequest request) {
        AchievementDefinition definition = definitions.findById(id).orElseThrow(() -> new NoSuchElementException("Achievement definition not found"));
        if (!definition.getCode().equals(request.code().trim().toUpperCase(Locale.ROOT))) throw new IllegalArgumentException("Achievement code cannot be changed after creation");
        definition.setTitle(request.title().trim()); definition.setDescription(request.description().trim());
        definition.setCategory(normalizeAchievementCategory(request.category())); definition.setThreshold(request.threshold()); definition.setActive(request.active());
        return toDefinition(definition);
    }

    @Transactional
    public void deactivateAchievementDefinition(Long id) {
        AchievementDefinition definition = definitions.findById(id).orElseThrow(() -> new NoSuchElementException("Achievement definition not found"));
        definition.setActive(false);
    }

    @Transactional
    public ChallengeTemplateResponse createTemplate(ChallengeTemplateRequest request) {
        validateTemplate(request, null);
        return toTemplate(templates.save(fromRequest(request, ChallengeTemplate.builder().build())));
    }

    @Transactional
    public ChallengeTemplateResponse updateTemplate(Long id, ChallengeTemplateRequest request) {
        ChallengeTemplate existing = templates.findById(id).orElseThrow(() -> new NoSuchElementException("Challenge template not found"));
        validateTemplate(request, id);
        return toTemplate(templates.save(fromRequest(request, existing)));
    }

    @Transactional
    public void deleteTemplate(Long id) {
        ChallengeTemplate existing = templates.findById(id).orElseThrow(() -> new NoSuchElementException("Challenge template not found"));
        if (submissions.existsByChallengeTemplateId(existing.getId())) {
            existing.setActive(false);
        } else {
            templates.delete(existing);
        }
    }

    @Transactional
    public void markAwardSeen(Long userId, Long awardId) {
        AchievementAward award = awards.findById(awardId).filter(item -> item.getUserId().equals(userId)).orElseThrow(() -> new NoSuchElementException("Achievement not found"));
        if (award.getSeenAt() == null) award.setSeenAt(LocalDateTime.now(Clock.systemUTC()));
    }

    @Transactional
    void refreshCountGoals(Long userId, LocalDateTime activityTime) {
        for (LearningGoal goal : goals.findByUserIdAndStatus(userId, GoalStatus.ACTIVE)) {
            if (goal.getDueOn() != null && goal.getDueOn().isBefore(LocalDate.now(PLATFORM_ZONE))) { goal.setStatus(GoalStatus.EXPIRED); continue; }
            if (goal.getMetric() == GoalMetric.CATEGORY_CHALLENGE_COMPLETIONS) {
                long count = submissions.countByUserIdAndCategorySnapshotAndSubmittedAtGreaterThanEqual(userId, goal.getCategory(), goal.getCreatedAt());
                goal.setProgress(BigDecimal.valueOf(count));
                completeGoalIfReached(goal, activityTime);
                continue;
            }
            ChallengeCadence cadence = switch (goal.getMetric()) {
                case DAILY_CHALLENGE_COMPLETIONS -> ChallengeCadence.DAILY;
                case WEEKLY_CHALLENGE_COMPLETIONS -> ChallengeCadence.WEEKLY;
                default -> null;
            };
            if (cadence == null) continue;
            long count = submissions.countByUserIdAndCadenceAndSubmittedAtGreaterThanEqual(userId, cadence, goal.getCreatedAt());
            goal.setProgress(BigDecimal.valueOf(count));
            completeGoalIfReached(goal, activityTime);
        }
    }

    private void refreshStreakGoals(Long userId, LocalDateTime activityTime) {
        int streak = currentStreak(activityDays.findActivityDates(userId));
        for (LearningGoal goal : goals.findByUserIdAndStatus(userId, GoalStatus.ACTIVE)) {
            if (goal.getMetric() != GoalMetric.CURRENT_STREAK_DAYS) continue;
            if (goal.getDueOn() != null && goal.getDueOn().isBefore(LocalDate.now(PLATFORM_ZONE))) { goal.setStatus(GoalStatus.EXPIRED); continue; }
            goal.setProgress(BigDecimal.valueOf(streak));
            completeGoalIfReached(goal, activityTime);
        }
    }

    private void expireGoals(Long userId) {
        LocalDate today = LocalDate.now(PLATFORM_ZONE);
        goals.findByUserIdAndStatus(userId, GoalStatus.ACTIVE).stream().filter(goal -> goal.getDueOn() != null && goal.getDueOn().isBefore(today)).forEach(goal -> goal.setStatus(GoalStatus.EXPIRED));
    }

    private void evaluateChallengeAchievements(Long userId) {
        long total = submissions.countByUserId(userId);
        int streak = currentStreak(activityDays.findActivityDates(userId));
        for (AchievementDefinition definition : definitions.findByActiveTrue()) {
            boolean earned = switch (definition.getCategory()) {
                case "CHALLENGE_COMPLETIONS" -> total >= definition.getThreshold();
                case "STREAK_DAYS" -> streak >= definition.getThreshold();
                case "WEEKLY_CHALLENGES" -> submissions.countByUserIdAndCadence(userId, ChallengeCadence.WEEKLY) >= definition.getThreshold();
                default -> false;
            };
            awardIfEarned(userId, definition, earned);
        }
    }

    private void evaluateAssessmentAchievements(Long userId, BigDecimal score, Map<String, BigDecimal> rubricScores, boolean personalBest) {
        long reviewed = assessmentEvents.countByUserId(userId);
        for (AchievementDefinition definition : definitions.findByActiveTrue()) {
            boolean earned = switch (definition.getCategory()) {
                case "REVIEWED_COMPOSITIONS" -> reviewed >= definition.getThreshold();
                case "OVERALL_SCORE" -> score != null && score.compareTo(BigDecimal.valueOf(definition.getThreshold())) >= 0;
                case "VOCABULARY_SCORE" -> rubricScores != null && rubricScores.get("VOCABULARY_WORD_CHOICE") != null
                        && rubricScores.get("VOCABULARY_WORD_CHOICE").compareTo(BigDecimal.valueOf(definition.getThreshold())) >= 0;
                case "PERSONAL_BEST_SCORE" -> personalBest && score != null && score.compareTo(BigDecimal.valueOf(definition.getThreshold())) >= 0;
                default -> false;
            };
            awardIfEarned(userId, definition, earned);
        }
    }

    private void validateTemplate(ChallengeTemplateRequest request, Long existingId) {
        if (request.maxResponseUnits() < request.minResponseUnits()) throw new IllegalArgumentException("Maximum response units must be at least the minimum");
        if ((existingId == null && templates.existsByCode(request.code().trim())
                || (existingId != null && templates.existsByCodeAndIdNot(request.code(), existingId))) ) throw new IllegalArgumentException("Challenge code is already in use");
    }

    private void recordActivityDay(Long userId, LocalDate activityDate) {
        if (!activityDays.existsByUserIdAndActivityDate(userId, activityDate)) {
            activityDays.save(LearningActivityDay.builder().userId(userId).activityDate(activityDate).build());
        }
    }

    private ChallengeTemplate fromRequest(ChallengeTemplateRequest request, ChallengeTemplate item) {
        item.setCode(request.code().trim()); item.setCadence(request.cadence()); item.setLanguage(normalizeLanguage(request.language()));
        String category = normalizeCategory(request.category());
        item.setCategory(category == null ? Optional.ofNullable(item.getCategory()).orElse("REFLECTION") : category);
        item.setThemePath(normalizeThemePath(request.themePath(), item.getThemePath()));
        item.setTitle(request.title().trim()); item.setPrompt(request.prompt().trim()); item.setMinResponseUnits(request.minResponseUnits());
        item.setMaxResponseUnits(request.maxResponseUnits()); item.setActive(request.active()); item.setDisplayOrder(request.displayOrder());
        return item;
    }

    private int currentStreak(List<LocalDate> dates) { return streakFrom(dates, LocalDate.now(PLATFORM_ZONE)); }
    private int longestStreak(List<LocalDate> dates) {
        if (dates.isEmpty()) return 0; List<LocalDate> ascending = new ArrayList<>(dates); Collections.sort(ascending);
        int longest = 0, running = 0; LocalDate previous = null;
        for (LocalDate date : ascending) { running = previous != null && date.equals(previous.plusDays(1)) ? running + 1 : 1; longest = Math.max(longest, running); previous = date; }
        return longest;
    }
    private int streakFrom(List<LocalDate> dates, LocalDate today) {
        Set<LocalDate> days = new HashSet<>(dates); LocalDate cursor = days.contains(today) ? today : today.minusDays(1); int streak = 0;
        while (days.contains(cursor)) { streak++; cursor = cursor.minusDays(1); } return streak;
    }
    private static boolean isScoreGoal(GoalMetric metric) { return metric == GoalMetric.OVERALL_ASSESSMENT_SCORE || metric == GoalMetric.VOCABULARY_RUBRIC_SCORE; }
    private static String normalizeLanguage(String language) { return "CHINESE".equalsIgnoreCase(language) ? "CHINESE" : "ENGLISH"; }
    private static String normalizeCategory(String category) {
        if (category == null || category.isBlank()) return null;
        String normalized = category.trim().toUpperCase(Locale.ROOT);
        if (!CHALLENGE_CATEGORIES.contains(normalized)) throw new IllegalArgumentException("Unsupported challenge category");
        return normalized;
    }
    private static String normalizeThemePath(String themePath, String existing) {
        if (themePath == null || themePath.isBlank()) return existing == null || existing.isBlank() ? "EVERYDAY_CONFIDENCE" : existing;
        String normalized = themePath.trim().toUpperCase(Locale.ROOT);
        if (!THEME_PATHS.contains(normalized)) throw new IllegalArgumentException("Unsupported themed path");
        return normalized;
    }
    private static String normalizeAchievementCategory(String category) {
        String normalized = category.trim().toUpperCase(Locale.ROOT);
        if (!ACHIEVEMENT_CATEGORIES.contains(normalized)) throw new IllegalArgumentException("Unsupported achievement category");
        return normalized;
    }
    /** Challenges accept the same rich-text HTML as compositions, but limits apply to what learners wrote—not markup. */
    private static String visibleText(String content) {
        return content.replaceAll("(?is)<(script|style)\\b[^>]*>.*?</\\1>", " ")
                .replaceAll("<[^>]*>", " ")
                .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'");
    }
    private static int responseUnits(String content, String language) { return "CHINESE".equals(language) ? (int) content.codePoints().filter(c -> Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN).count() : content.trim().isEmpty() ? 0 : content.trim().split("\\s+").length; }
    private static String unitLabel(String language) { return "CHINESE".equals(language) ? " Chinese characters" : " words"; }
    private static void completeGoalIfReached(LearningGoal goal, LocalDateTime completedAt) {
        if (goal.getProgress().compareTo(goal.getTarget()) >= 0) { goal.setStatus(GoalStatus.COMPLETED); goal.setCompletedAt(completedAt); goal.setFocused(false); }
    }
    private void awardIfEarned(Long userId, AchievementDefinition definition, boolean earned) {
        if (earned && !awards.existsByUserIdAndAchievementDefinitionId(userId, definition.getId())) awards.save(AchievementAward.builder().userId(userId).achievementDefinitionId(definition.getId()).awardedAt(LocalDateTime.now(Clock.systemUTC())).build());
    }
    private ChallengeResponse toChallenge(ChallengeTemplate item, String cycleKey, boolean submitted) { return new ChallengeResponse(item.getId(), item.getCadence(), item.getLanguage(), item.getCategory(), item.getThemePath(), item.getTitle(), item.getPrompt(), item.getMinResponseUnits(), item.getMaxResponseUnits(), cycleKey, submitted); }
    private ChallengeTemplateResponse toTemplate(ChallengeTemplate item) { return new ChallengeTemplateResponse(item.getId(), item.getCode(), item.getCadence(), item.getLanguage(), item.getCategory(), item.getThemePath(), item.getTitle(), item.getPrompt(), item.getMinResponseUnits(), item.getMaxResponseUnits(), item.isActive(), item.getDisplayOrder()); }
    private ChallengeSubmissionResponse toSubmission(ChallengeSubmission item) { return new ChallengeSubmissionResponse(item.getId(), item.getChallengeTemplateId(), item.getCadence(), item.getActivityDate(), item.getTitleSnapshot(), item.getContent(), item.getSubmittedAt()); }
    private GoalResponse toGoal(LearningGoal item) { return new GoalResponse(item.getId(), item.getMetric(), item.getCategory(), item.isFocused(), item.getTarget(), item.getProgress(), item.getStatus(), item.getStartsOn(), item.getDueOn(), item.getCreatedAt(), item.getCompletedAt()); }
    private AchievementDefinitionResponse toDefinition(AchievementDefinition definition) { return new AchievementDefinitionResponse(definition.getId(), definition.getCode(), definition.getTitle(), definition.getDescription(), definition.getCategory(), definition.getThreshold(), definition.isActive()); }
    private AchievementResponse toAchievement(AchievementAward award, AchievementDefinition definition) { return definition == null ? null : new AchievementResponse(award.getId(), definition.getCode(), definition.getTitle(), definition.getDescription(), definition.getCategory(), definition.getThreshold(), award.getAwardedAt(), award.getSeenAt()); }
}
