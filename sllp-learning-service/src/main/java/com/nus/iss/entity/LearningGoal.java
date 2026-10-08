package com.nus.iss.entity;

import com.nus.iss.enums.GoalMetric;
import com.nus.iss.enums.GoalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "learning_goal")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LearningGoal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 48) private GoalMetric metric;
    @Column(length = 32) private String category;
    @Column(nullable = false, precision = 7, scale = 2) private BigDecimal target;
    @Column(nullable = false, precision = 7, scale = 2) @Builder.Default private BigDecimal progress = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) @Builder.Default private GoalStatus status = GoalStatus.ACTIVE;
    @Column(nullable = false) @Builder.Default private boolean focused = false;
    @Column(name = "starts_on", nullable = false) private LocalDate startsOn;
    @Column(name = "due_on") private LocalDate dueOn;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "completed_at") private LocalDateTime completedAt;
}
