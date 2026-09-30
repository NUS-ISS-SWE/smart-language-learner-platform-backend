package com.nus.iss.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/** One row per learner and Singapore calendar day, regardless of activity source. */
@Entity
@Table(name = "learning_activity_day", uniqueConstraints = @UniqueConstraint(name = "uq_learning_activity_day", columnNames = {"user_id", "activity_date"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LearningActivityDay {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "activity_date", nullable = false) private LocalDate activityDate;
}
