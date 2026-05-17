package com.nus.iss.entity;

import com.nus.iss.enums.CompositionSource;
import com.nus.iss.enums.CompositionStatus;
import com.nus.iss.enums.Language;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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
    private String username;
    private String title;
    private String content;
    @Enumerated(EnumType.STRING)
    private CompositionSource compositionSource;
    @Enumerated(EnumType.STRING)
    private Language language;
    @Enumerated(EnumType.STRING)
    private CompositionStatus compositionStatus;
    private String systemFeedback;
    private String storagePath;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
