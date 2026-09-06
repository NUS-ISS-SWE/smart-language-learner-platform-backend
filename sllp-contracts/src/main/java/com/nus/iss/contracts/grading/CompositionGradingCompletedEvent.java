package com.nus.iss.contracts.grading;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Versioned payload sent after asynchronous composition grading completes. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompositionGradingCompletedEvent {
    private UUID requestId;
    private LocalDateTime requestedAt;
    private Long compositionId;
    private Long userId;
    private BigDecimal score;
    private String feedback;
    private boolean success;
    private String failureReason;
    private String suggestionStatus;
    private List<ImprovementSuggestion> suggestions;
    private LocalDateTime completedAt;
    private Long promptTokens;
    private Long completionTokens;
    private Long cachedInputTokens;
    private Long totalTokens;
    private Long estimatedCostMicros;
    private String aiProvider;
    private String aiModel;
    private String pricingVersion;
    private Long inputPriceMicrosPerMillion;
    private Long cachedInputPriceMicrosPerMillion;
    private Long outputPriceMicrosPerMillion;
}
