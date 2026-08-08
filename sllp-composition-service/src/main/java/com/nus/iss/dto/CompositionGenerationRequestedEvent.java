package com.nus.iss.dto;

import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompositionGenerationRequestedEvent {
    private Long compositionId;
    private Long userId;
    private String title;
    private String description;
    private String framework;
    private DifficultyLevel difficultyLevel;
    private Language language;
    private LocalDateTime requestedAt;
}
