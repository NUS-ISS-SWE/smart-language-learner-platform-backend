package com.nus.iss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompositionGenerationCompletedEvent {
    private Long compositionId;
    private String username;
    private String generatedContent;
    private boolean success;
    private String failureReason;
    private LocalDateTime completedAt;
}
