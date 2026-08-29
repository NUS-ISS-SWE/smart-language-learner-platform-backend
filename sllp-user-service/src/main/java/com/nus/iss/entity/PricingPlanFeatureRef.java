package com.nus.iss.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="pricing_plan_feature") @Getter @Setter @NoArgsConstructor
public class PricingPlanFeatureRef { @Id private Long id; @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="pricing_plan_id") private PricingPlanRef pricingPlan; @Column(name="feature_text") private String featureText; @Column(name="display_order") private Integer displayOrder; }
