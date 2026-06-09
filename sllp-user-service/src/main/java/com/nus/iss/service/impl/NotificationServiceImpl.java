package com.nus.iss.service.impl;

import com.nus.iss.entity.AppUser;
import com.nus.iss.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private final JavaMailSender javaMailSender;

    @Autowired
    public NotificationServiceImpl(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void sendActivationEmail(AppUser savedAppUser) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(savedAppUser.getEmail());
        message.setSubject("Activate Your Smart Language Learner Platform Account");
        message.setText(
                "Dear " + savedAppUser.getUsername() + ",\n\n" +
                        "Welcome to Smart Language Learner Platform!\n\n" +
                        "Thank you for registering with our Smart Language Learner Platform. " +
                        "To complete your registration, please activate your account by clicking the link below:\n\n" +
                        "https://www.nusmtechswe.com/api/v1/auth/activate?token=" +
                        Base64.getEncoder().encodeToString(savedAppUser.getEmail().getBytes()) + "\n\n" +
                        "If you did not create an account with us, please disregard this email.\n\n" +
                        "Best regards,\n" +
                        "Smart Language Learner Platform Team"
        );
        javaMailSender.send(message);
        log.info("Sent activation email to the user {}", savedAppUser.getEmail());
    }

    @Override
    public void sendDeletionEmail(AppUser appUser) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(appUser.getEmail());
        message.setSubject("Your Smart Language Learner Platform Account Has Been Deleted");
        message.setText(
                "Dear " + appUser.getUsername() + ",\n\n" +
                        "We would like to inform you that your account on the Smart Language Learner Platform has been successfully deleted.\n\n" +
                        "If you initiated this request, no further action is required.\n\n" +
                        "If you did not request this deletion or believe this was done in error, please contact our support team immediately.\n\n" +
                        "Thank you for being part of our platform. You are always welcome to register again in the future.\n\n" +
                        "Best regards,\n" +
                        "Smart Language Learner Platform Team"
        );
        javaMailSender.send(message);
    }

    @Override
    public void sendForgotPasswordEmail(AppUser user) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Reset Your Smart Language Learner Platform Password");
        message.setText(
                "Dear " + user.getUsername() + ",\n\n" +
                        "We received a request to reset your password for your Smart Language Learner Platform account.\n\n" +
                        "Please click the link below to reset your password:\n\n" +
                        "https://www.nusmtechswe.com/reset-password?token=" +
                        Base64.getEncoder().encodeToString(user.getEmail().getBytes()) + "\n\n" +
                        "If you did not request a password reset, please ignore this email.\n\n" +
                        "Best regards,\n" +
                        "Smart Language Learner Platform Team"
        );
        javaMailSender.send(message);
    }
}
