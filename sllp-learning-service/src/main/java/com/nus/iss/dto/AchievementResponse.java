package com.nus.iss.dto;

import java.time.LocalDateTime;

public record AchievementResponse(Long awardId, String code, String title, String description,
                                  String category, int threshold, LocalDateTime awardedAt,
                                  LocalDateTime seenAt) { }
