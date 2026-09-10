package com.nus.iss.controller;

import com.nus.iss.config.StripeProperties;
import com.nus.iss.service.SubscriptionService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives Stripe webhook events. Not routed through the gateway's JWT filter —
 * Stripe's requests carry no bearer token, only an HMAC signature in the
 * {@code Stripe-Signature} header, which is verified below. We never call the
 * Stripe API from this service; this endpoint only verifies and reacts to events.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/subscription")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final SubscriptionService subscriptionService;
    private final StripeProperties stripeProperties;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeProperties.getWebhookSecret());
        } catch (SignatureVerificationException | RuntimeException e) {
            // RuntimeException also covers a malformed (non-JSON) payload.
            log.warn("Rejected Stripe webhook — {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid payload or signature");
        }

        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
            String clientReferenceId = session != null ? session.getClientReferenceId() : null;

            if (clientReferenceId == null) {
                log.warn("checkout.session.completed event {} has no client_reference_id — cannot map to a user",
                        event.getId());
            } else {
                subscriptionService.confirmPendingSubscription(Long.valueOf(clientReferenceId), session.getId());
            }
        }

        // Always return 2xx once the event is verified, or Stripe will keep retrying.
        return ResponseEntity.ok("received");
    }
}
