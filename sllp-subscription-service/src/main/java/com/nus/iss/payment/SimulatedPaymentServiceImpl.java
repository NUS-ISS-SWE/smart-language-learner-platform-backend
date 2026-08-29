package com.nus.iss.payment;

import com.nus.iss.entity.PricingPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Simulated payment implementation — always approves instantly.
 * Replace with (or override using {@code @Primary}) a real provider
 * implementation once the Stripe integration is ready.
 */
@Slf4j
@Service
public class SimulatedPaymentServiceImpl implements PaymentService {

    @Override
    public PaymentResult processPayment(Long userId, PricingPlan plan, String paymentToken) {
        String reference = "SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Simulated payment approved for user {} plan {} — reference {}", userId, plan, reference);
        return PaymentResult.success(reference);
    }
}
