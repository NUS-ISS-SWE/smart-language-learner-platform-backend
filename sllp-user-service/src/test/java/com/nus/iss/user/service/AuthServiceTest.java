package com.nus.iss.user.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.nus.iss.user.dto.AuthResponse;
import com.nus.iss.user.dto.LoginRequest;
import com.nus.iss.user.dto.RegisterRequest;
import com.nus.iss.user.entity.User;
import com.nus.iss.user.exception.InvalidCredentialsException;
import com.nus.iss.user.exception.UserAlreadyExistsException;
import com.nus.iss.user.exception.UserNotFoundException;
import com.nus.iss.user.repository.UserRepository;
import com.nus.iss.user.security.JwtProvider;

/**
 * Unit tests for AuthService.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("Test User", "test@example.com", "password123");
        loginRequest = new LoginRequest("test@example.com", "password123");

        testUser = User.builder()
            .id("550e8400-e29b-41d4-a716-446655440000")
            .name("Test User")
            .email("test@example.com")
            .passwordHash("hashedPassword")
            .role("STUDENT")
            .active(true)
            .emailVerified(false)
            .build();
    }

    @Test
    void testRegister_Success() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.password())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtProvider.generateAccessToken(anyString(), anyString(), anyString()))
            .thenReturn("token123");

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals(testUser.getId(), response.userId());
        assertEquals(testUser.getEmail(), response.email());
        assertEquals("Bearer", response.tokenType());
        verify(userRepository).save(any(User.class));
        verify(emailService).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void testRegister_UserAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(true);

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLogin_Success() {
        // Arrange
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.password(), testUser.getPasswordHash())).thenReturn(true);
        when(jwtProvider.generateAccessToken(anyString(), anyString(), anyString()))
            .thenReturn("token123");

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals(testUser.getId(), response.userId());
        assertEquals(testUser.getEmail(), response.email());
    }

    @Test
    void testLogin_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> authService.login(loginRequest));
    }

    @Test
    void testLogin_InvalidPassword() {
        // Arrange
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.password(), testUser.getPasswordHash())).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    void testLogin_UserInactive() {
        // Arrange
        testUser.setActive(false);
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.password(), testUser.getPasswordHash())).thenReturn(true);

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest));
    }
}
