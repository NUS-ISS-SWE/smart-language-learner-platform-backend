package com.nus.iss.service.impl;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nus.iss.config.JwtConfig;
import com.nus.iss.dto.AppUserDTO;
import com.nus.iss.dto.JwtAccessTokenDTO;
import com.nus.iss.dto.UserProfileDTO;
import com.nus.iss.entity.AppUser;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.PreferredLanguage;
import com.nus.iss.enums.Role;
import com.nus.iss.enums.Status;
import com.nus.iss.exception.UserAlreadyExistsException;
import com.nus.iss.repository.AppUserRepository;
import com.nus.iss.service.AppUserService;
import com.nus.iss.service.NotificationService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AppUserServiceImpl implements AppUserService {

    private final AppUserRepository appUserRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;

    @Autowired
    public AppUserServiceImpl(AppUserRepository appUserRepository, NotificationService notificationService, PasswordEncoder passwordEncoder, JwtConfig jwtConfig) {
        this.appUserRepository = appUserRepository;
        this.notificationService = notificationService;
        this.passwordEncoder = passwordEncoder;
        this.jwtConfig = jwtConfig;
    }

    @Override
    @Transactional
    public AppUserDTO registerUser(AppUserDTO appUserDTO) {
        log.info("Registering user with username: {} and email: {}", appUserDTO.getUsername(), appUserDTO.getEmail());

        if (appUserRepository.existsByUsername(appUserDTO.getUsername()) || appUserRepository.existsByEmail(appUserDTO.getEmail())) {
            throw new UserAlreadyExistsException("Account already exists");
        }

        AppUser appUser = AppUser.builder()
                .username(appUserDTO.getUsername())
                .email(appUserDTO.getEmail())
                .password(passwordEncoder.encode(appUserDTO.getPassword()))
                .status(Status.PENDING_EMAIL_VERIFICATION)
                .role(Role.USER_FREE_TRIAL)
                .preferredLanguage(appUserDTO.getPreferredLanguage() != null ? appUserDTO.getPreferredLanguage() : PreferredLanguage.ENGLISH)
                .difficultyLevel(appUserDTO.getDifficultyLevel() != null ? appUserDTO.getDifficultyLevel() : DifficultyLevel.BEGINNER)
                .createdAt(LocalDateTime.now())
                .aiTokenBalance(5000L)
                .aiTokenUsed(0L)
                .build();

        AppUser savedUser = appUserRepository.save(appUser);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        try {
            notificationService.sendActivationEmail(savedUser);
        } catch (Exception e) {
            log.warn("Failed to send activation email to {}: {}", savedUser.getEmail(), e.getMessage());
        }

        return mapToDTO(savedUser);
    }

    @Override
    public JwtAccessTokenDTO login(String username, String password) {
        log.info("Login attempt for user: {}", username);
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (user.getStatus() != Status.ACTIVE) {
            throw new BadCredentialsException("User account is not active");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String token = jwtConfig.generateToken(user.getUsername(), user.getId(), user.getRole().name());

        return JwtAccessTokenDTO.builder()
                .accessToken(token)
                .role(user.getRole().name())
                .build();
    }

    @Override
    public void forgotPassword(String email) {
        log.info("Forgot password request for email: {}", email);
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("User not found with email: " + email));
        notificationService.sendForgotPasswordEmail(user);
    }

    @Override
    @Transactional
    public JwtAccessTokenDTO confirmForgotPassword(String token, String newPassword) {
        log.info("Confirming forgot password with token");
        String email = new String(Base64.getDecoder().decode(token));
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired reset token"));
        user.setPassword(passwordEncoder.encode(newPassword));
        appUserRepository.save(user);
        String jwtToken = jwtConfig.generateToken(user.getUsername(), user.getId(), user.getRole().name());
        log.info("Password reset successfully via token for user: {}", user.getUsername());
        return JwtAccessTokenDTO.builder()
                .accessToken(jwtToken)
                .role(user.getRole().name())
                .build();
    }

    @Override
    @Transactional
    public void resetPassword(AppUserDTO appUserDTO) {
        log.info("Reset password request for user: {}", appUserDTO.getUsername());
        AppUser user = appUserRepository.findByUsername(appUserDTO.getUsername())
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        if (!passwordEncoder.matches(appUserDTO.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Old password does not match");
        }

        user.setPassword(passwordEncoder.encode(appUserDTO.getNewPassword()));
        appUserRepository.save(user);
        log.info("Password reset successfully for user: {}", user.getUsername());
    }

    @Override
    @Transactional
    public void activateUser(String token) {
        log.info("Activating user with token: {}", token);
        try {
            String email = new String(Base64.getDecoder().decode(token));
            AppUser user = appUserRepository.findByEmail(email)
                    .orElseThrow(() -> new BadCredentialsException("Invalid activation token"));

            if (user.getStatus() == Status.PENDING_EMAIL_VERIFICATION) {
                user.setStatus(Status.ACTIVE);
                appUserRepository.save(user);
                log.info("User activated successfully: {}", email);
            } else {
                log.warn("User already active or in another state: {}", email);
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid Base64 token: {}", token);
            throw new BadCredentialsException("Invalid activation token");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(Long userId) {
        if (userId == null || userId < 1) {
            throw new IllegalArgumentException("Valid user ID is required");
        }

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User profile not found"));

        return UserProfileDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .difficultyLevel(user.getDifficultyLevel())
                .build();
    }

    private AppUserDTO mapToDTO(AppUser user) {
        return AppUserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .status(user.getStatus())
                .role(user.getRole())
                .preferredLanguage(user.getPreferredLanguage())
                .difficultyLevel(user.getDifficultyLevel())
                .createdAt(user.getCreatedAt())
                .aiTokenBalance(user.getAiTokenBalance())
                .aiTokenUsed(user.getAiTokenUsed())
                .build();
    }
}
