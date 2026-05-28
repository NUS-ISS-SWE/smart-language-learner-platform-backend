package com.nus.iss.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nus.iss.user.dto.AuthResponse;
import com.nus.iss.user.dto.LoginRequest;
import com.nus.iss.user.dto.RegisterRequest;
import com.nus.iss.user.dto.UserDTO;
import com.nus.iss.user.security.JwtAuthenticationToken;
import com.nus.iss.user.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for authentication endpoints.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthController {

    private final AuthService authService;

    /**
     * User registration endpoint.
     *
     * @param request registration request with name, email, password
     * @return JWT access token and user info
     */
    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Create new user account with email and password")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register endpoint called for email: {}", request.email());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * User login endpoint.
     *
     * @param request login request with email and password
     * @return JWT access token and user info
     */
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login endpoint called for email: {}", request.email());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Verify email endpoint (uses embedded token in URL for MVP).
     *
     * @param token verification token
     * @return success message
     */
    @PostMapping("/verify-email")
    @Operation(summary = "Verify email", description = "Verify user email address")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        log.info("Email verification endpoint called");
        authService.verifyEmail(token);
        return ResponseEntity.ok("Email verified successfully");
    }

    /**
     * Get current user profile.
     *
     * @param authentication current user authentication
     * @return user profile DTO
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Retrieve authenticated user profile")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            UserDTO user = authService.getCurrentUser(jwtAuth.getUserId());
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
