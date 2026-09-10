package com.nus.iss.service;

import java.util.List;

import com.nus.iss.dto.SelectPlanRequestDTO;
import com.nus.iss.dto.SubscriptionPlanInfoDTO;
import com.nus.iss.dto.UserSubscriptionDTO;

public interface SubscriptionService {

    UserSubscriptionDTO getCurrentSubscription(Long userId);

    List<UserSubscriptionDTO> getSubscriptionHistory(Long userId);

    UserSubscriptionDTO selectPlan(Long userId, SelectPlanRequestDTO request);

    UserSubscriptionDTO cancelSubscription(Long userId);

    List<SubscriptionPlanInfoDTO> getAvailablePlans();

    /** Activates a user's PENDING subscription once Stripe confirms payment via webhook. */
    void confirmPendingSubscription(Long userId, String stripeReference);
}
