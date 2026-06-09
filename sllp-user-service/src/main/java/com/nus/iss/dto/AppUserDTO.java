package com.nus.iss.dto;

import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.PreferredLanguage;
import com.nus.iss.enums.Role;
import com.nus.iss.enums.Status;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AppUserDTO {
    private Long id;

    @NotBlank(message = "Username cannot be blank")
    private String username;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, message = "Password should have at least 6 characters")
    @ToString.Exclude
    private String password;

    @ToString.Exclude
    private String newPassword;

    private Status status;
    private PreferredLanguage preferredLanguage;
    private DifficultyLevel difficultyLevel;
    private LocalDateTime createdAt;
    private Role role;
    private Long token;
    private Long totalTokenConsumption;
    private LocalDate subscriptionStartDate;
    private LocalDate subscriptionEndDate;
    private Integer subscriptionDurationInDays;
}