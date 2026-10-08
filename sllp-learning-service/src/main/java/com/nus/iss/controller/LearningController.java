package com.nus.iss.controller;

import com.nus.iss.dto.*;
import com.nus.iss.enums.ChallengeCadence;
import com.nus.iss.service.LearningProgressService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/learning")
public class LearningController {
    private final LearningProgressService service;
    public LearningController(LearningProgressService service) { this.service = service; }
    @GetMapping("/summary") public LearningSummaryResponse summary(@RequestHeader("X-Sllp-User-Id") Long userId, @RequestParam(defaultValue = "ENGLISH") String language) { return service.summary(userId, language); }
    @GetMapping("/challenges/current") public ChallengeResponse current(@RequestHeader("X-Sllp-User-Id") Long userId, @RequestParam ChallengeCadence cadence, @RequestParam(defaultValue = "ENGLISH") String language, @RequestParam(required = false) String themePath) { return service.currentChallenge(userId, cadence, language, themePath); }
    @PostMapping("/challenges/{challengeId}/submissions") public ResponseEntity<ChallengeSubmissionResponse> submit(@RequestHeader("X-Sllp-User-Id") Long userId, @PathVariable Long challengeId, @RequestParam(defaultValue = "ENGLISH") String language, @Valid @RequestBody ChallengeSubmissionRequest request) { return ResponseEntity.ok(service.submit(userId, challengeId, request, language)); }
    @GetMapping("/challenges/{challengeId}/submissions/current") public ChallengeSubmissionResponse currentSubmission(@RequestHeader("X-Sllp-User-Id") Long userId, @PathVariable Long challengeId, @RequestParam(defaultValue = "ENGLISH") String language) { return service.currentSubmission(userId, challengeId, language); }
    @GetMapping("/goals") public List<GoalResponse> goals(@RequestHeader("X-Sllp-User-Id") Long userId) { return service.listGoals(userId); }
    @PostMapping("/goals") public ResponseEntity<GoalResponse> createGoal(@RequestHeader("X-Sllp-User-Id") Long userId, @Valid @RequestBody CreateGoalRequest request) { return ResponseEntity.ok(service.createGoal(userId, request)); }
    @PostMapping("/goals/{goalId}/cancel") public GoalResponse cancelGoal(@RequestHeader("X-Sllp-User-Id") Long userId, @PathVariable Long goalId) { return service.cancelGoal(userId, goalId); }
    @PostMapping("/goals/{goalId}/focus") public GoalResponse focus(@RequestHeader("X-Sllp-User-Id") Long userId, @PathVariable Long goalId) { return service.focusGoal(userId, goalId); }
    @GetMapping("/achievements") public List<AchievementResponse> achievements(@RequestHeader("X-Sllp-User-Id") Long userId) { return service.listAchievements(userId); }
    @PostMapping("/achievements/{awardId}/seen") public ResponseEntity<Void> seen(@RequestHeader("X-Sllp-User-Id") Long userId, @PathVariable Long awardId) { service.markAwardSeen(userId, awardId); return ResponseEntity.noContent().build(); }
}
