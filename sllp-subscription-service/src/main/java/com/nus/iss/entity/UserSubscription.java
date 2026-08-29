package com.nus.iss.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.nus.iss.enums.SubscriptionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pricing_plan_id", nullable = false)
    private PricingPlan pricingPlan;

    @Column(name = "plan_code_snapshot", nullable = false, length = 50) private String planCodeSnapshot;
    @Column(name = "plan_name_snapshot", nullable = false, length = 120) private String planNameSnapshot;
    @Column(name = "price_minor_snapshot", nullable = false) private Long priceMinorSnapshot;
    @Column(name = "currency_snapshot", nullable = false, length = 3) private String currencySnapshot;
    @Column(name = "features_snapshot", nullable = false, columnDefinition = "TEXT") private String featuresSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SubscriptionStatus status;

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer durationDays;

    /** Stripe transaction / charge ID — populated by the Stripe developer's implementation. */
    @Column(name = "payment_reference", length = 255)
    private String paymentReference;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = SubscriptionStatus.ACTIVE;
        }
    }
}
