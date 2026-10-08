package com.nus.iss.dto;

import java.util.List;

public record LearningSummaryResponse(int currentStreak, int longestStreak, long completedChallenges,
                                      List<GoalResponse> activeGoals, List<AchievementResponse> newAwards,
                                      ChallengeResponse dailyChallenge, ChallengeResponse weeklyChallenge) { }
