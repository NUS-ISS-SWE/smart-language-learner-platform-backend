package com.nus.iss.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "composition_moderation_flag", indexes = {
        @Index(name = "idx_composition_moderation_flag_composition", columnList = "composition_id"),
        @Index(name = "idx_composition_moderation_flag_created", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompositionModerationFlag {
    @Id
    private UUID id;
    @Column(name = "request_id", nullable = false, length = 255)
    private String requestId;
    @Column(name = "context_id", nullable = false, length = 255)
    private String contextId;
    @Column(name = "composition_id")
    private Long compositionId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "content_sha256", nullable = false, length = 64)
    private String contentSha256;
    @Column(name = "content_length", nullable = false)
    private int contentLength;
    @Column(name = "stage", nullable = false, length = 32)
    private String stage;
    @Column(name = "decision", nullable = false, length = 32)
    private String decision;
    @Column(name = "provider_response_id", length = 255)
    private String providerResponseId;
    @Column(name = "model", nullable = false, length = 255)
    private String model;
    @Column(name = "provider_flagged", nullable = false)
    private boolean providerFlagged;
    @Column(name = "matched_category_scores", nullable = false, columnDefinition = "TEXT")
    private String matchedCategoryScores;
    @Column(name = "threshold", nullable = false)
    private double threshold;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
