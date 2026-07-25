package com.nus.iss.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordConfirmDTO(
        @NotBlank(message = "Token cannot be blank")
        String token,

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String newPassword
) {}
