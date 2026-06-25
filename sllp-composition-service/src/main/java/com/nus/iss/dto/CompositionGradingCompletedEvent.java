package com.nus.iss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompositionGradingCompletedEvent {
    private Long compositionId;
    private Long userId;
    private BigDecimal score;
    private String feedback;
    private boolean success;
    private String failureReason;
    private LocalDateTime completedAt;
}
