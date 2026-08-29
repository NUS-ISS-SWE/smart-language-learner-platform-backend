package com.nus.iss.entity;
import java.util.*;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "pricing_plan") @Getter @Setter @NoArgsConstructor
public class PricingPlanRef {
 @Id private Long id;
 @Column(nullable=false) private String code;
 @Column(name="display_name",nullable=false) private String displayName;
 @Column(name="price_minor",nullable=false) private Long priceMinor;
 @Column(nullable=false) private String currency;
 @Column(name="duration_days") private Integer durationDays;
 @Column(name="access_role",nullable=false) private String accessRole;
 @Column(nullable=false) private boolean active;
 @OneToMany(mappedBy="pricingPlan",fetch=FetchType.EAGER) @OrderBy("displayOrder ASC") private List<PricingPlanFeatureRef> features=new ArrayList<>();
}
