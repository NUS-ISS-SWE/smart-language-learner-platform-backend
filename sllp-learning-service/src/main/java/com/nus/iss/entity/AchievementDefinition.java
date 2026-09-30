package com.nus.iss.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "achievement_definition")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AchievementDefinition {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 100) private String code;
    @Column(nullable = false, length = 255) private String title;
    @Column(nullable = false, length = 500) private String description;
    @Column(nullable = false, length = 48) private String category;
    @Column(nullable = false) private int threshold;
    @Column(nullable = false) @Builder.Default private boolean active = true;
}
