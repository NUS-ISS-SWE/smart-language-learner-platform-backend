package com.nus.iss.contracts.grading;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Versioned payload sent when a composition requires asynchronous grading. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompositionGradingRequestedEvent {
    private UUID requestId;
    private Long compositionId;
    private Long userId;
    private String title;
    private String content;
    private String language;
    private LocalDateTime requestedAt;
}
