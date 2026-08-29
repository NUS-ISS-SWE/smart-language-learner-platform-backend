package com.nus.iss.payment;

import com.nus.iss.entity.PricingPlan;

/**
 * Abstraction over payment processing. The current implementation is a
 * simulated stub ({@link SimulatedPaymentServiceImpl}) that always succeeds.
 *
 * <p><strong>Stripe integration hook:</strong> the Stripe developer should
 * add a {@code StripePaymentServiceImpl} that implements this interface,
 * annotate it with {@code @Primary}, and use {@code request.paymentToken()}
 * as the Stripe PaymentIntent ID to confirm the charge. The
 * {@link com.nus.iss.dto.SelectPlanRequestDTO#paymentToken()} field and the
 * {@code payment_reference} column on {@code user_subscription} exist
 * specifically for this purpose.
 */
public interface PaymentService {

    /**
     * Process a payment for the given user and plan.
     *
     * @param userId       the application user ID
     * @param plan         the subscription plan being purchased
     * @param paymentToken optional client-supplied token (e.g. Stripe PaymentIntent ID)
     * @return a {@link PaymentResult} indicating success or failure
     */
    PaymentResult processPayment(Long userId, PricingPlan plan, String paymentToken);
}
