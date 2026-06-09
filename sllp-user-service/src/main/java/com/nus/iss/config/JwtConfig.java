package com.nus.iss.config;

import com.nus.iss.entity.AppUser;
import com.nus.iss.repository.AppUserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class JwtConfig {
    private final SecretKey key;
    private final String issuer;
    private final long expirationTimeMillis;
    private final AppUserRepository appUserRepository;

    public JwtConfig(
            @Value("${sllp.security.jwt.issuer}") String issuer,
            @Value("${sllp.security.jwt.secret}") String secret,
            @Value("${sllp.security.jwt.expirationTimeMillis:86400000}") long expirationTimeMillis,
            AppUserRepository appUserRepository) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = issuer;
        this.expirationTimeMillis = expirationTimeMillis;
        this.appUserRepository = appUserRepository;
    }

    public String generateToken(String username, String role) {
        Map<String, String> claims = new HashMap<>();
        claims.put("ROLE", role);
        String token = Jwts.builder()
                .claims(claims)
                .issuer(issuer)
                .subject(username)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusMillis(expirationTimeMillis)))
                .signWith(key)
                .compact();
        log.info("Token generated for user {}: {}", username, token);
        return token;
    }

    public String extractUsername(String jwt) {
        Claims claims = getClaims(jwt);
        return claims.getSubject();
    }

    private Claims getClaims(String jwt) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }

    public boolean isTokenValid(String jwt) {
        Claims claims = getClaims(jwt);
        String subject = claims.getSubject();
        Optional<AppUser> user = appUserRepository.findByUsername(subject);
        if (user.isPresent()) {
            if (claims.getExpiration().after(Date.from(Instant.now()))) {
                log.info("User {} validated successfully", subject);
                return true;
            } else {
                log.error("Token expired for user {}", subject);
                return false;
            }
        } else {
            log.error("User {} not exist in the database", subject);
            return false;
        }
    }
}
