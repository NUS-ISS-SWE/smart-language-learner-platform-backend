package com.nus.iss.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Standard error response DTO.
 *
 * @param code error code identifier
 * @param message user-friendly error message
 */
public record ErrorResponse(
    @Schema(description = "Error code", example = "USER_NOT_FOUND")
    String code,

    @Schema(description = "Error message", example = "User with email not found")
    String message
) { }
