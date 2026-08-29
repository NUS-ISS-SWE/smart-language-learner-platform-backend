package com.nus.iss.service;
import java.util.List;
import com.nus.iss.dto.*;
import com.nus.iss.entity.PricingPlan;
public interface PricingPlanService { List<SubscriptionPlanInfoDTO> listActive(); List<SubscriptionPlanInfoDTO> listAll(); SubscriptionPlanInfoDTO create(PricingPlanRequest request); SubscriptionPlanInfoDTO update(String code, PricingPlanRequest request); SubscriptionPlanInfoDTO setStatus(String code, boolean active); void updateOrder(PricingPlanOrderRequest request); PricingPlan findActive(String code); PricingPlan findFreePlan(); }
