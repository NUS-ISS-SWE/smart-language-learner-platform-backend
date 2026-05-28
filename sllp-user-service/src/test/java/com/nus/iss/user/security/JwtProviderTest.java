package com.nus.iss.user.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for JwtProvider.
 */
class JwtProviderTest {

    private JwtProvider jwtProvider;
    private static final String TEST_SECRET = "your-secret-key-change-in-production-min-32-chars";

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtProvider, "accessTokenExpiry", 900000L);
        ReflectionTestUtils.setField(jwtProvider, "refreshTokenExpiry", 604800000L);
    }

    @Test
    void testGenerateAccessToken() {
        // Act
        String token = jwtProvider.generateAccessToken("user-123", "user@example.com", "STUDENT");

        // Assert
        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertTrue(jwtProvider.validateToken(token));
    }

    @Test
    void testValidateToken_Valid() {
        // Arrange
        String token = jwtProvider.generateAccessToken("user-123", "user@example.com", "STUDENT");

        // Act & Assert
        assertTrue(jwtProvider.validateToken(token));
    }

    @Test
    void testValidateToken_Invalid() {
        // Arrange
        String invalidToken = "invalid.token.here";

        // Act & Assert
        assertFalse(jwtProvider.validateToken(invalidToken));
    }

    @Test
    void testGetUserIdFromToken() {
        // Arrange
        String token = jwtProvider.generateAccessToken("user-123", "user@example.com", "STUDENT");

        // Act
        String userId = jwtProvider.getUserIdFromToken(token);

        // Assert
        assertEquals("user-123", userId);
    }

    @Test
    void testGetEmailFromToken() {
        // Arrange
        String token = jwtProvider.generateAccessToken("user-123", "user@example.com", "STUDENT");

        // Act
        String email = jwtProvider.getEmailFromToken(token);

        // Assert
        assertEquals("user@example.com", email);
    }

    @Test
    void testGetRoleFromToken() {
        // Arrange
        String token = jwtProvider.generateAccessToken("user-123", "user@example.com", "ADMIN");

        // Act
        String role = jwtProvider.getRoleFromToken(token);

        // Assert
        assertEquals("ADMIN", role);
    }
}
