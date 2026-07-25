package com.nus.iss.controller;

import com.nus.iss.dto.SelectPlanRequestDTO;
import com.nus.iss.dto.SubscriptionPlanInfoDTO;
import com.nus.iss.dto.UserSubscriptionDTO;
import com.nus.iss.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private static final String USER_ID_HEADER = "X-Sllp-User-Id";

    private final SubscriptionService subscriptionService;

    /** Public — no authentication required. Lists all available plans with pricing. */
    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlanInfoDTO>> getAvailablePlans() {
        return ResponseEntity.ok(subscriptionService.getAvailablePlans());
    }

    /** Protected — returns the caller's current subscription, running an expiry check on access. */
    @GetMapping("/current")
    public ResponseEntity<UserSubscriptionDTO> getCurrentSubscription(
            @RequestHeader(USER_ID_HEADER) Long userId) {
        return ResponseEntity.ok(subscriptionService.getCurrentSubscription(userId));
    }

    /** Protected — selects or upgrades to a plan; cancels any current active subscription first. */
    @PostMapping("/select")
    public ResponseEntity<UserSubscriptionDTO> selectPlan(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody SelectPlanRequestDTO request) {
        return ResponseEntity.ok(subscriptionService.selectPlan(userId, request));
    }

    /** Protected — cancels the active subscription and downgrades the user to FREE_TRIAL. */
    @PostMapping("/cancel")
    public ResponseEntity<UserSubscriptionDTO> cancelSubscription(
            @RequestHeader(USER_ID_HEADER) Long userId) {
        return ResponseEntity.ok(subscriptionService.cancelSubscription(userId));
    }

    /** Protected — returns all past and current subscriptions for the caller. */
    @GetMapping("/history")
    public ResponseEntity<List<UserSubscriptionDTO>> getSubscriptionHistory(
            @RequestHeader(USER_ID_HEADER) Long userId) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionHistory(userId));
    }
}
