package com.nus.iss.user.service;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Email service for sending verification and notification emails.
 * Stub implementation - integrate with actual email service (SendGrid, AWS SES, etc.)
 */
@Service
@Slf4j
public class EmailService {

    /**
     * Send email verification email.
     *
     * @param email user email
     * @param userId user ID (for token generation)
     */
    public void sendVerificationEmail(String email, String userId) {
        log.info("Sending verification email to: {}", email);
        // TODO: Implement actual email sending via SMTP or email service provider
        // For MVP/testing: stub that logs the email
    }

    /**
     * Send password reset email.
     *
     * @param email user email
     */
    public void sendPasswordResetEmail(String email) {
        log.info("Sending password reset email to: {}", email);
        // TODO: Implement actual email sending
    }
}
