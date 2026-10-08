package com.nus.iss.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.nus.iss.enums.ModerationAction;
import com.nus.iss.enums.ModerationReasonCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Immutable, per-composition moderation history. */
@Entity
@Table(name = "composition_moderation_decision")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompositionModerationDecision {
    @Id
    private UUID id;

    @Column(name = "composition_id", nullable = false)
    private Long compositionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ModerationAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_category", length = 32)
    private ModerationReasonCategory reasonCategory;

    @Column(name = "reason_note", length = 500)
    private String reasonNote;

    @Column(name = "moderator_id", nullable = false)
    private Long moderatorId;

    @Column(name = "moderator_name", length = 255)
    private String moderatorName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
