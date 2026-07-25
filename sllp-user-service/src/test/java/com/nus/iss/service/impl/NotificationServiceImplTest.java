package com.nus.iss.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.nus.iss.entity.AppUser;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Test
    void sendActivationEmailBuildsActivationMessage() {
        NotificationServiceImpl service = new NotificationServiceImpl(javaMailSender, "http://localhost:8080", "http://localhost:3000");
        AppUser user = appUser();

        service.sendActivationEmail(user);

        SimpleMailMessage message = sentMessage();
        assertArrayEquals(new String[]{"ming@example.com"}, message.getTo());
        assertEquals("Activate Your Smart Language Learner Platform Account", message.getSubject());
        assertTrue(message.getText().contains("Dear ming,"));
        assertTrue(message.getText().contains("http://localhost:8080/api/v1/auth/activate?token=" + encodedEmail()));
    }

    @Test
    void sendDeletionEmailBuildsDeletionMessage() {
        NotificationServiceImpl service = new NotificationServiceImpl(javaMailSender, "http://localhost:8080", "http://localhost:3000");
        AppUser user = appUser();

        service.sendDeletionEmail(user);

        SimpleMailMessage message = sentMessage();
        assertArrayEquals(new String[]{"ming@example.com"}, message.getTo());
        assertEquals("Your Smart Language Learner Platform Account Has Been Deleted", message.getSubject());
        assertTrue(message.getText().contains("Dear ming,"));
        assertTrue(message.getText().contains("successfully deleted"));
    }

    @Test
    void sendForgotPasswordEmailBuildsResetPasswordMessage() {
        NotificationServiceImpl service = new NotificationServiceImpl(javaMailSender, "http://localhost:8080", "http://localhost:3000");
        AppUser user = appUser();

        service.sendForgotPasswordEmail(user);

        SimpleMailMessage message = sentMessage();
        assertArrayEquals(new String[]{"ming@example.com"}, message.getTo());
        assertEquals("Reset Your Smart Language Learner Platform Password", message.getSubject());
        assertTrue(message.getText().contains("Dear ming,"));
        assertTrue(message.getText().contains("http://localhost:3000/reset-password?token=" + encodedEmail()));
    }

    private SimpleMailMessage sentMessage() {
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(messageCaptor.capture());
        return messageCaptor.getValue();
    }

    private static AppUser appUser() {
        return AppUser.builder()
                .username("ming")
                .email("ming@example.com")
                .build();
    }

    private static String encodedEmail() {
        return Base64.getEncoder().encodeToString("ming@example.com".getBytes(StandardCharsets.UTF_8));
    }
}
