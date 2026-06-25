package com.nus.iss.config;

import com.nus.iss.entity.AppUser;
import com.nus.iss.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

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
        String token = jwtConfig.generateToken("ming", "USER_FREE_TRIAL");

        assertEquals("ming", jwtConfig.extractUsername(token));
    }

    @Test
    void isTokenValid_whenSubjectExistsAndTokenIsNotExpiredReturnsTrue() {
        String token = jwtConfig.generateToken("ming", "USER_FREE_TRIAL");
        when(appUserRepository.findByUsername("ming"))
                .thenReturn(Optional.of(AppUser.builder().username("ming").build()));

        assertTrue(jwtConfig.isTokenValid(token));
    }

    @Test
    void isTokenValid_whenSubjectDoesNotExistReturnsFalse() {
        String token = jwtConfig.generateToken("missing", "USER_FREE_TRIAL");
        when(appUserRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertFalse(jwtConfig.isTokenValid(token));
    }
}
