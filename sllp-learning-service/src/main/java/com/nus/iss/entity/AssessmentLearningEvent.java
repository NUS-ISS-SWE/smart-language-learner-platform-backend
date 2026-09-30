package com.nus.iss.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "assessment_learning_event")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AssessmentLearningEvent {
    @Id @Column(name = "request_id") private UUID requestId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "composition_id", nullable = false) private Long compositionId;
    @Column(precision = 5, scale = 2) private BigDecimal score;
    @Column(name = "completed_at", nullable = false) private LocalDateTime completedAt;
}
