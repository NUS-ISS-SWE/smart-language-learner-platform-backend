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
@Table(name = "lakera_guard_detection", indexes = {
        @Index(name = "idx_lakera_guard_detection_request", columnList = "request_id"),
        @Index(name = "idx_lakera_guard_detection_composition", columnList = "composition_id"),
        @Index(name = "idx_lakera_guard_detection_created", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LakeraGuardDetection {
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
    @Column(name = "screened_text", nullable = false, columnDefinition = "TEXT")
    private String screenedText;
    @Column(name = "provider_request_uuid", length = 255)
    private String providerRequestUuid;
    @Column(name = "action", length = 32)
    private String action;
    @Column(name = "provider_flagged", nullable = false)
    private boolean providerFlagged;
    @Column(name = "detector_breakdown", nullable = false, columnDefinition = "TEXT")
    private String detectorBreakdown;
    @Column(name = "provider_response", nullable = false, columnDefinition = "TEXT")
    private String providerResponse;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
