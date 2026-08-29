package com.nus.iss.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "recommendation", indexes = {
    @Index(name = "idx_recommendation_user_id", columnList = "user_id"),
    @Index(name = "idx_recommendation_created_at", columnList = "created_at")
})
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String recommendationType; // GRAMMAR, VOCABULARY, STRUCTURE, COMPLEXITY

    @Column(columnDefinition = "TEXT")
    private String recommendation; // Specific recommendation text

    @Column(precision = 5, scale = 2)
    private java.math.BigDecimal performanceScore; // User's average score

    @Column(columnDefinition = "TEXT")
    private String reasoning; // Why this recommendation was made

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
