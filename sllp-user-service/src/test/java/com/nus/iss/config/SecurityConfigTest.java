package com.nus.iss.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private AppUserDetailsService appUserDetailsService;

    @Test
    void passwordEncoderReturnsBcryptEncoder() {
        SecurityConfig securityConfig = new SecurityConfig(appUserDetailsService);

        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        String encodedPassword = passwordEncoder.encode("password123");

        assertNotEquals("password123", encodedPassword);
        assertTrue(passwordEncoder.matches("password123", encodedPassword));
    }
}
