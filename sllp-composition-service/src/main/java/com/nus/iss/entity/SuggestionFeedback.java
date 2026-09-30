package com.nus.iss.entity;

import com.nus.iss.enums.SuggestionFeedbackRating;
import com.nus.iss.enums.SuggestionFeedbackReason;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity @Table(name = "suggestion_feedback", uniqueConstraints = @UniqueConstraint(columnNames = {"composition_id", "user_id", "suggestion_index"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SuggestionFeedback {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "composition_id", nullable = false) private Long compositionId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "suggestion_index", nullable = false) private Integer suggestionIndex;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private SuggestionFeedbackRating rating;
    @Enumerated(EnumType.STRING) @Column(length = 20) private SuggestionFeedbackReason reason;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
}
