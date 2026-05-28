package com.nus.iss.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Authentication response DTO.
 *
 * @param accessToken JWT access token
 * @param tokenType token type (always "Bearer")
 * @param userId authenticated user ID
 * @param email user email
 * @param name user full name
 * @param role user role (STUDENT, TEACHER, ADMIN)
 */
public record AuthResponse(
    @Schema(description = "JWT access token", example = "eyJhbGc...")
    String accessToken,

    @Schema(description = "Token type", example = "Bearer")
    String tokenType,

    @Schema(description = "User ID", example = "550e8400-e29b-41d4-a716-446655440000")
    String userId,

    @Schema(description = "User email", example = "user@example.com")
    String email,

    @Schema(description = "User full name", example = "John Doe")
    String name,

    @Schema(description = "User role", example = "STUDENT")
    String role
) { }
