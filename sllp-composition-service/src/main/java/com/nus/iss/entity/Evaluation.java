package com.nus.iss.entity;

import jakarta.persistence.*;
import com.nus.iss.contracts.grading.ImprovementSuggestion;
import com.nus.iss.persistence.ImprovementSuggestionListConverter;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "evaluation", indexes = {
    @Index(name = "idx_evaluation_composition_id", columnList = "composition_id")
})
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "composition_id", nullable = false)
    private Long compositionId;

    @Column(name = "request_id", unique = true)
    private UUID requestId;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "suggestion_status", length = 64)
    private String suggestionStatus;

    @Convert(converter = ImprovementSuggestionListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<ImprovementSuggestion> suggestions;

    @Column(nullable = false)
    @Builder.Default
    private boolean success = true;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;

    @PrePersist
    protected void onCreate() {
        if (evaluatedAt == null) {
            evaluatedAt = LocalDateTime.now();
        }
    }
}
