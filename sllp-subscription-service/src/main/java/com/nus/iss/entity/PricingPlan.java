package com.nus.iss.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "pricing_plan")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PricingPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 50, updatable = false) private String code;
    @Column(name = "display_name", nullable = false, length = 120) private String displayName;
    private String description;
    @Column(name = "price_minor", nullable = false) private Long priceMinor;
    @Column(nullable = false, length = 3) private String currency;
    @Column(name = "billing_interval", nullable = false) private String billingInterval;
    @Column(name = "duration_days") private Integer durationDays;
    @Column(name = "access_role", nullable = false) private String accessRole;
    @Column(nullable = false) private boolean active;
    @Column(name = "display_order", nullable = false) private Integer displayOrder;
    @Builder.Default @Column(name = "monthly_assessment_limit", nullable = false) private Integer monthlyAssessmentLimit = 20;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @OneToMany(mappedBy = "pricingPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC") @Builder.Default private List<PricingPlanFeature> features = new ArrayList<>();
    @PrePersist void create() { var now = LocalDateTime.now(); if (createdAt == null) createdAt = now; if (updatedAt == null) updatedAt = now; }
    @PreUpdate void update() { updatedAt = LocalDateTime.now(); }
}
