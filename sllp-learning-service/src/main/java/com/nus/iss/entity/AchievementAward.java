package com.nus.iss.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "achievement_award", uniqueConstraints = @UniqueConstraint(name = "uq_achievement_award_user_definition", columnNames = {"user_id", "achievement_definition_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AchievementAward {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "achievement_definition_id", nullable = false) private Long achievementDefinitionId;
    @Column(name = "awarded_at", nullable = false) private LocalDateTime awardedAt;
    @Column(name = "seen_at") private LocalDateTime seenAt;
}
