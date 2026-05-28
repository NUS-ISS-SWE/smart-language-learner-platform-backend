package com.nus.iss.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nus.iss.user.dto.AuthResponse;
import com.nus.iss.user.dto.LoginRequest;
import com.nus.iss.user.dto.RegisterRequest;
import com.nus.iss.user.dto.UserDTO;
import com.nus.iss.user.entity.User;
import com.nus.iss.user.exception.InvalidCredentialsException;
import com.nus.iss.user.exception.UserAlreadyExistsException;
import com.nus.iss.user.exception.UserNotFoundException;
import com.nus.iss.user.repository.UserRepository;
import com.nus.iss.user.security.JwtProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Authentication service for user registration and login.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final EmailService emailService;

    /**
     * Register new user.
     *
     * @param request registration request
     * @return authentication response with JWT token
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering user with email: {}", request.email());

        // Check if user already exists
        if (userRepository.existsByEmail(request.email())) {
            log.warn("User with email {} already exists", request.email());
            throw new UserAlreadyExistsException(request.email());
        }

        // Create user entity
        User user = User.builder()
            .name(request.name())
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .role("STUDENT")
            .active(true)
            .emailVerified(false)  // Require email verification
            .build();

        // Save user
        user = userRepository.save(user);
        log.info("User registered successfully with ID: {}", user.getId());

        // Generate email verification token (stub for now - in real app would send email)
        emailService.sendVerificationEmail(user.getEmail(), user.getId());

        // Generate JWT tokens
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());

        return new AuthResponse(
            accessToken,
            "Bearer",
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getRole()
        );
    }

    /**
     * User login.
     *
     * @param request login request
     * @return authentication response with JWT token
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("User login attempt for email: {}", request.email());

        // Find user by email
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new UserNotFoundException(request.email()));

        // Verify password
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Invalid password for user: {}", request.email());
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Check if user is active
        if (!user.isActive()) {
            log.warn("User {} is inactive", request.email());
            throw new InvalidCredentialsException("User account is inactive");
        }

        log.info("User logged in successfully: {}", request.email());

        // Generate JWT tokens
        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole());

        return new AuthResponse(
            accessToken,
            "Bearer",
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getRole()
        );
    }

    /**
     * Get current user profile.
     *
     * @param userId user ID
     * @return user DTO
     */
    @Transactional(readOnly = true)
    public UserDTO getCurrentUser(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));

        return new UserDTO(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.isEmailVerified(),
            user.getCreatedAt()
        );
    }

    /**
     * Verify email (stub implementation - in real app would validate token).
     *
     * @param token verification token
     */
    @Transactional
    public void verifyEmail(String token) {
        log.info("Email verification requested with token");
        // Stub: In production, validate token against database
        // For MVP: assume verification succeeds
    }
}
