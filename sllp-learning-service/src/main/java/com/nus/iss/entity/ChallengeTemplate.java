package com.nus.iss.entity;

import com.nus.iss.enums.ChallengeCadence;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "challenge_template")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ChallengeTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 100) private String code;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private ChallengeCadence cadence;
    @Column(nullable = false, length = 20) private String language;
    @Column(nullable = false, length = 32) @Builder.Default private String category = "REFLECTION";
    @Column(name = "theme_path", nullable = false, length = 64) @Builder.Default private String themePath = "EVERYDAY_CONFIDENCE";
    @Column(nullable = false, length = 255) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String prompt;
    @Column(name = "min_response_units", nullable = false) private int minResponseUnits;
    @Column(name = "max_response_units", nullable = false) private int maxResponseUnits;
    @Column(nullable = false) @Builder.Default private boolean active = true;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @PrePersist void createTimestamps() { LocalDateTime now = LocalDateTime.now(); if (createdAt == null) createdAt = now; if (updatedAt == null) updatedAt = now; }
    @PreUpdate void updateTimestamp() { updatedAt = LocalDateTime.now(); }
}
