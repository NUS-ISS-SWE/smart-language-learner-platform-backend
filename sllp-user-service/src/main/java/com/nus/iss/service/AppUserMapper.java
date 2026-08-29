package com.nus.iss.service;

import com.nus.iss.dto.AdminUserResponseDTO;
import com.nus.iss.dto.AppUserDTO;
import com.nus.iss.dto.UserProfileDTO;
import com.nus.iss.entity.AppUser;

/** Centralizes API projections of the user entity. */
public class AppUserMapper {

    public AppUserDTO toUserDto(AppUser user) {
        return AppUserDTO.builder()
                .id(user.getId()).username(user.getUsername()).email(user.getEmail()).status(user.getStatus())
                .role(user.getRole()).preferredLanguage(user.getPreferredLanguage()).difficultyLevel(user.getDifficultyLevel())
                .createdAt(user.getCreatedAt()).aiTokenBalance(user.getAiTokenBalance()).aiTokenUsed(user.getAiTokenUsed())
                .build();
    }

    public UserProfileDTO toProfileDto(AppUser user) {
        return UserProfileDTO.builder().id(user.getId()).username(user.getUsername())
                .difficultyLevel(user.getDifficultyLevel()).build();
    }

    public AdminUserResponseDTO toAdminDto(AppUser user) {
        return new AdminUserResponseDTO(user.getId(), user.getUsername(), user.getEmail(),
                user.getRole() == null ? null : user.getRole().name(),
                user.getStatus() == null ? null : user.getStatus().name(),
                user.getPreferredLanguage() == null ? null : user.getPreferredLanguage().name(),
                user.getDifficultyLevel() == null ? null : user.getDifficultyLevel().name(),
                user.getAiTokenBalance(), user.getAiTokenUsed(), user.getCreatedAt(), user.getDeletedAt());
    }
}
