package com.nus.iss.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "user_subscription")
public class UserSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "pricing_plan_id", nullable = false)
    private Long pricingPlanId;

    @Column(name = "plan_code_snapshot", nullable = false, length = 50)
    private String planCodeSnapshot;

    @Column(name = "plan_name_snapshot", nullable = false, length = 120)
    private String planNameSnapshot;

    @Column(name = "price_minor_snapshot", nullable = false)
    private Long priceMinorSnapshot;

    @Column(name = "currency_snapshot", nullable = false, length = 3)
    private String currencySnapshot;

    @Column(name = "features_snapshot", nullable = false, columnDefinition = "TEXT")
    private String featuresSnapshot;

    @Column(nullable = false, length = 50)
    private String status;

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer durationDays;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
