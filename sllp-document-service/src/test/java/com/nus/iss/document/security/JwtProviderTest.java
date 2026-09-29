package com.nus.iss.document.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtProviderTest {

    private static final String SECRET = "unit-test-secret-that-is-long-enough-for-hmac-sha256";

    private final JwtProvider jwtProvider = new JwtProvider();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtProvider, "jwtSecret", SECRET);
    }

    @Test
    void validTokenIsAcceptedAndItsClaimsAreRead() {
        String token = Jwts.builder()
                .subject("alex")
                .claim("userId", 42)
                .claim("email", "alex@example.com")
                .claim("ROLE", "USER_BASIC")
                .signWith(key(SECRET))
                .compact();

        assertTrue(jwtProvider.validateToken(token));
        assertEquals("42", jwtProvider.getUserIdFromToken(token));
        assertEquals("alex@example.com", jwtProvider.getEmailFromToken(token));
        assertEquals("USER_BASIC", jwtProvider.getRoleFromToken(token));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String token = Jwts.builder()
                .claim("userId", 42)
                .signWith(key("another-secret-that-is-also-long-enough-for-hmac"))
                .compact();

        assertFalse(jwtProvider.validateToken(token));
    }

    @Test
    void expiredTokenIsRejected() {
        String token = Jwts.builder()
                .claim("userId", 42)
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(key(SECRET))
                .compact();

        assertFalse(jwtProvider.validateToken(token));
    }

    @Test
    void malformedOrEmptyTokenIsRejected() {
        assertFalse(jwtProvider.validateToken("not-a-jwt"));
        assertFalse(jwtProvider.validateToken(""));
    }

    @Test
    void tokenWithoutUserIdCannotIdentifyTheUser() {
        String token = Jwts.builder()
                .subject("alex")
                .signWith(key(SECRET))
                .compact();

        assertThrows(IllegalArgumentException.class, () -> jwtProvider.getUserIdFromToken(token));
        assertNull(jwtProvider.getRoleFromToken(token));
    }

    private static javax.crypto.SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
