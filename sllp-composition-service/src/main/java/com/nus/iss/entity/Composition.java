package com.nus.iss.entity;

import com.nus.iss.enums.CompositionSource;
import com.nus.iss.enums.CompositionStatus;
import com.nus.iss.enums.Language;
import com.nus.iss.enums.ModerationReasonCategory;
import com.nus.iss.enums.ModerationStatus;
import com.nus.iss.contracts.grading.ImprovementSuggestion;
import com.nus.iss.persistence.ImprovementSuggestionListConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "composition")
public class Composition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    private String title;
    private String content;
    @Enumerated(EnumType.STRING)
    private CompositionSource compositionSource;
    @Enumerated(EnumType.STRING)
    private Language language;
    @Enumerated(EnumType.STRING)
    private CompositionStatus compositionStatus;
    private String systemFeedback;
    private String suggestionStatus;
    @Convert(converter = ImprovementSuggestionListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<ImprovementSuggestion> suggestions;
    private String gradingProgressMessage;
    private String gradingProgressStage;
    private String storagePath;
    /**
     * Optional opaque ID of the document from which this composition was created.
     * Documents are owned by another service, so this is deliberately not a local
     * foreign key. The document service still enforces access when it is read.
     */
    @Column(name = "source_document_id")
    private String sourceDocumentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 16)
    @Builder.Default
    private ModerationStatus moderationStatus = ModerationStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_reason_category", length = 32)
    private ModerationReasonCategory moderationReasonCategory;

    @Column(name = "moderation_reason_note", length = 500)
    private String moderationReasonNote;

    @Column(name = "moderated_by_id")
    private Long moderatedById;

    @Column(name = "moderated_by_name", length = 255)
    private String moderatedByName;

    @Column(name = "moderated_at")
    private LocalDateTime moderatedAt;

    @Version
    private Long version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
