package com.nus.iss.document.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

/**
 * JWT provider for validating tokens (read-only, inherits from user-service).
 */
@Component
@Slf4j
public class JwtProvider {

    @Value("${application.jwt.secret}")
    private String jwtSecret;

    /**
     * Validate JWT token.
     *
     * @param token JWT token
     * @return true if valid
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException | ExpiredJwtException | 
                 UnsupportedJwtException | IllegalArgumentException e) {
            log.error("JWT validation failed", e);
        }
        return false;
    }

    /**
     * Get user ID from JWT.
     *
     * @param token JWT token
     * @return user ID
     */
    public String getUserIdFromToken(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
    }

    /**
     * Get email from JWT.
     *
     * @param token JWT token
     * @return email
     */
    public String getEmailFromToken(String token) {
        return (String) Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .get("email");
    }

    /**
     * Get role from JWT.
     *
     * @param token JWT token
     * @return role
     */
    public String getRoleFromToken(String token) {
        return (String) Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .get("role");
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}
