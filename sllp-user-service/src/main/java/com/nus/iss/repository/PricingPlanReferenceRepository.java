package com.nus.iss.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.nus.iss.entity.PricingPlanRef;
public interface PricingPlanReferenceRepository extends JpaRepository<PricingPlanRef,Long> { Optional<PricingPlanRef> findByCodeAndActiveTrue(String code); }
