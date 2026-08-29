package com.nus.iss.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "pricing_plan_feature") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PricingPlanFeature {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "pricing_plan_id", nullable = false) private PricingPlan pricingPlan;
 @Column(name = "feature_text", nullable = false) private String featureText;
 @Column(name = "display_order", nullable = false) private Integer displayOrder;
}
