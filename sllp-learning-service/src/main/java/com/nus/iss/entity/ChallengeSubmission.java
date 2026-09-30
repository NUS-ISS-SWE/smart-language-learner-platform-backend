package com.nus.iss.entity;

import com.nus.iss.enums.ChallengeCadence;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "challenge_submission", uniqueConstraints = @UniqueConstraint(name = "uq_challenge_submission_cycle", columnNames = {"user_id", "cadence", "cycle_key"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ChallengeSubmission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "challenge_template_id", nullable = false) private Long challengeTemplateId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private ChallengeCadence cadence;
    @Column(name = "cycle_key", nullable = false, length = 32) private String cycleKey;
    @Column(name = "activity_date", nullable = false) private LocalDate activityDate;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @Column(name = "title_snapshot", nullable = false, length = 255) private String titleSnapshot;
    @Column(name = "prompt_snapshot", nullable = false, columnDefinition = "TEXT") private String promptSnapshot;
    @Column(name = "category_snapshot", nullable = false, length = 32) private String categorySnapshot;
    @Column(name = "submitted_at", nullable = false) private LocalDateTime submittedAt;
}
