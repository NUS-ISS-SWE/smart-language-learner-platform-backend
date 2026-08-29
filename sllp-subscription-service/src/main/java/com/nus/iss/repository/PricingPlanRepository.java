package com.nus.iss.repository;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import com.nus.iss.entity.PricingPlan;
@Repository public interface PricingPlanRepository extends JpaRepository<PricingPlan, Long> {
 List<PricingPlan> findByActiveTrueOrderByDisplayOrderAsc();
 List<PricingPlan> findAllByOrderByDisplayOrderAsc();
 Optional<PricingPlan> findByCode(String code);
 Optional<PricingPlan> findByCodeAndActiveTrue(String code);
}
