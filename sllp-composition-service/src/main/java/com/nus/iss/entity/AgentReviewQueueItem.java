package com.nus.iss.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "agent_review_queue")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AgentReviewQueueItem {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "request_id", nullable = false, unique = true) private UUID requestId;
    @Column(name = "composition_id", nullable = false) private Long compositionId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false, length = 24) @Builder.Default private String status = "OPEN";
    @Column(name = "failure_reason", nullable = false, length = 500) private String failureReason;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
}
