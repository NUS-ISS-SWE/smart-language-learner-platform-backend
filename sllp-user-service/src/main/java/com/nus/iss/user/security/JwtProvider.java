package com.nus.iss.user.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT token provider for generating and validating JWT tokens.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtProvider {

    @Value("${application.jwt.secret}")
    private String jwtSecret;

    @Value("${application.jwt.access-token-expiry}")
    private long accessTokenExpiry;

    @Value("${application.jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    /**
     * Generate JWT access token.
     *
     * @param userId user ID
     * @param email user email
     * @param role user role
     * @return JWT token
     */
    public String generateAccessToken(String userId, String email, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("ROLE", role);
        return createToken(claims, userId, accessTokenExpiry);
    }

    /**
     * Generate JWT refresh token.
     *
     * @param userId user ID
     * @return JWT refresh token
     */
    public String generateRefreshToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userId, refreshTokenExpiry);
    }

    /**
     * Validate JWT token.
     *
     * @param token JWT token to validate
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            getJwtParser().parseSignedClaims(token);
            return true;
        } catch (SecurityException e) {
            log.error("Invalid JWT signature", e);
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token", e);
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token", e);
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token", e);
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty", e);
        }
        return false;
    }

    /**
     * Get user ID from JWT token.
     *
     * @param token JWT token
     * @return user ID (subject)
     */
    public String getUserIdFromToken(String token) {
        return getJwtParser().parseSignedClaims(token).getPayload().getSubject();
    }

    /**
     * Get email from JWT token.
     *
     * @param token JWT token
     * @return user email
     */
    public String getEmailFromToken(String token) {
        return (String) getJwtParser().parseSignedClaims(token).getPayload().get("email");
    }

    /**
     * Get role from JWT token.
     *
     * @param token JWT token
     * @return user role
     */
    public String getRoleFromToken(String token) {
        return (String) getJwtParser().parseSignedClaims(token).getPayload().get("ROLE");
    }

    /**
     * Create JWT token with claims.
     *
     * @param claims token claims
     * @param subject token subject (user ID)
     * @param expiry token expiry in milliseconds
     * @return JWT token
     */
    private String createToken(Map<String, Object> claims, String subject, long expiry) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiry);

        return Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact();
    }

    /**
     * Get JWT parser instance.
     *
     * @return configured JWT parser
     */
    private JwtParser getJwtParser() {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build();
    }

    /**
     * Get signing key for JWT.
     *
     * @return signing key
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
