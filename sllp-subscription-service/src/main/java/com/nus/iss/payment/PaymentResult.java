package com.nus.iss.payment;

/**
 * Outcome of a payment attempt.
 *
 * <p>Use the factory methods {@link #success(String)} and {@link #failure(String)}
 * rather than the canonical constructor.
 */
public record PaymentResult(
        boolean success,
        String reference,
        String errorMessage
) {
    public static PaymentResult success(String reference) {
        return new PaymentResult(true, reference, null);
    }

    public static PaymentResult failure(String errorMessage) {
        return new PaymentResult(false, null, errorMessage);
    }
}
