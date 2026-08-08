package com.nus.iss.config;

import java.util.Optional;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nus.iss.entity.AppUser;
import com.nus.iss.repository.AppUserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@ExtendWith(MockitoExtension.class)
class JwtConfigTest {

    private static final String SECRET = "01234567890123456789012345678901";

    @Mock
    private AppUserRepository appUserRepository;

    private JwtConfig jwtConfig;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig("sllp-test", SECRET, 3_600_000L, appUserRepository);
    }

    @Test
    void extractUsername_whenTokenWasGeneratedReturnsSubject() {
        String token = jwtConfig.generateToken("ming", 1L, "USER_FREE_TRIAL");

        assertEquals("ming", jwtConfig.extractUsername(token));
        Claims claims = parseClaims(token);
        assertEquals("1", claims.get("userId", String.class));
        assertEquals("USER_FREE_TRIAL", claims.get("ROLE", String.class));
    }

    @Test
    void isTokenValid_whenSubjectExistsAndTokenIsNotExpiredReturnsTrue() {
        String token = jwtConfig.generateToken("ming", 1L, "USER_FREE_TRIAL");
        when(appUserRepository.findByUsername("ming"))
                .thenReturn(Optional.of(AppUser.builder().username("ming").build()));

        assertTrue(jwtConfig.isTokenValid(token));
    }

    @Test
    void isTokenValid_whenSubjectDoesNotExistReturnsFalse() {
        String token = jwtConfig.generateToken("missing", 1L, "USER_FREE_TRIAL");
        when(appUserRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertFalse(jwtConfig.isTokenValid(token));
    }

    private static Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
