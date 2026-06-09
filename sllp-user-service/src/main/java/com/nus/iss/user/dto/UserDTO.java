package com.nus.iss.user.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * User data transfer object.
 *
 * @param id user ID
 * @param name user full name
 * @param email user email
 * @param role user role
 * @param emailVerified whether email has been verified
 * @param createdAt account creation timestamp
 */
public record UserDTO(
    @Schema(description = "User ID", example = "550e8400-e29b-41d4-a716-446655440000")
    String id,

    @Schema(description = "User full name", example = "John Doe")
    String name,

    @Schema(description = "User email", example = "user@example.com")
    String email,

    @Schema(description = "User role", example = "STUDENT")
    String role,

    @Schema(description = "Email verification status")
    boolean emailVerified,

    @Schema(description = "Account creation timestamp")
    Instant createdAt
) { }
