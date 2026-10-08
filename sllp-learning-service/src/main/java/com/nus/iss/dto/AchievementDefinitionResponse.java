package com.nus.iss.dto;

public record AchievementDefinitionResponse(Long id, String code, String title, String description,
                                            String category, int threshold, boolean active) { }
