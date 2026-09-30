package com.nus.iss.dto;

import com.nus.iss.enums.ChallengeCadence;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ChallengeSubmissionResponse(
        Long id, Long challengeTemplateId, ChallengeCadence cadence, LocalDate activityDate,
        String title, String content, LocalDateTime submittedAt) { }
