package com.nus.iss.entity;

import com.nus.iss.enums.AssessmentQuotaStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "assessment_quota_reservation")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssessmentQuotaReservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "request_id", nullable = false, unique = true) private UUID requestId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "plan_code", nullable = false, length = 50) private String planCode;
    @Column(name = "cycle_start", nullable = false) private LocalDate cycleStart;
    @Column(name = "assessment_limit", nullable = false) private Integer assessmentLimit;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) private AssessmentQuotaStatus status;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
}
