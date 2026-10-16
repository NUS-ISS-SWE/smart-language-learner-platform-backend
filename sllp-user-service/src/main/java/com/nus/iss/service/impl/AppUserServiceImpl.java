package com.nus.iss.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.NoSuchElementException;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nus.iss.config.JwtConfig;
import com.nus.iss.dto.AppUserDTO;
import com.nus.iss.dto.JwtAccessTokenDTO;
import com.nus.iss.dto.UserProfileDTO;
import com.nus.iss.entity.AppUser;
import com.nus.iss.entity.PricingPlanRef;
import com.nus.iss.entity.UserSubscription;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.PreferredLanguage;
import com.nus.iss.enums.Role;
import com.nus.iss.enums.Status;
import com.nus.iss.exception.UserAlreadyExistsException;
import com.nus.iss.repository.AppUserRepository;
import com.nus.iss.repository.PricingPlanReferenceRepository;
import com.nus.iss.service.AppUserService;
import com.nus.iss.service.AppUserMapper;
import com.nus.iss.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppUserServiceImpl implements AppUserService {

    private final AppUserRepository appUserRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;
    private final PricingPlanReferenceRepository pricingPlanRepository;
    private final AppUserMapper appUserMapper = new AppUserMapper();

    @Override
    @Transactional
    public AppUserDTO registerUser(AppUserDTO appUserDTO) {
        log.info("Registering user with username: {} and email: {}", appUserDTO.getUsername(), appUserDTO.getEmail());

        if (appUserRepository.existsByUsername(appUserDTO.getUsername()) || appUserRepository.existsByEmail(appUserDTO.getEmail())) {
            throw new UserAlreadyExistsException("Account already exists");
        }

        PricingPlanRef selectedPlan = resolvePaidPlan(appUserDTO.getPlanType());
        Role role = selectedPlan == null ? Role.USER_FREE_TRIAL : Role.valueOf(selectedPlan.getAccessRole());

        AppUser appUser = AppUser.builder()
                .username(appUserDTO.getUsername())
                .email(appUserDTO.getEmail())
                .password(passwordEncoder.encode(appUserDTO.getPassword()))
                .status(Status.PENDING_EMAIL_VERIFICATION)
                .role(role)
                .preferredLanguage(appUserDTO.getPreferredLanguage() != null ? appUserDTO.getPreferredLanguage() : PreferredLanguage.ENGLISH)
                .difficultyLevel(appUserDTO.getDifficultyLevel() != null ? appUserDTO.getDifficultyLevel() : DifficultyLevel.BEGINNER)
                .createdAt(LocalDateTime.now())
                .aiTokenBalance(5000L)
                .aiTokenUsed(0L)
                .build();

        AppUser savedUser = appUserRepository.save(appUser);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        if (selectedPlan != null) createInitialSubscription(savedUser, selectedPlan);

        try {
            notificationService.sendActivationEmail(savedUser);
        } catch (Exception e) {
            log.warn("Failed to send activation email for userId={} category={}",
                    savedUser.getId(), e.getClass().getSimpleName());
        }

        return mapToDTO(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
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

        return buildJwtToken(user);
    }

    @Override
    @Transactional(readOnly = true)
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
        String email = decodeEmailToken(token);
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired reset token"));
        user.setPassword(passwordEncoder.encode(newPassword));
        appUserRepository.save(user);
        log.info("Password reset successfully via token for user: {}", user.getUsername());
        return buildJwtToken(user);
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
        log.info("Activation request received");
        String email = decodeEmailToken(token);
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid activation token"));

        if (user.getStatus() == Status.PENDING_EMAIL_VERIFICATION) {
            user.setStatus(Status.ACTIVE);
            appUserRepository.save(user);
            log.info("User activated successfully: {}", email);
        } else {
            log.warn("User already active or in another state: {}", email);
        }
    }

    private String decodeEmailToken(String token) {
        try {
            return new String(Base64.getDecoder().decode(token));
        } catch (IllegalArgumentException e) {
            log.warn("Received an invalid activation or reset token");
            throw new BadCredentialsException("Invalid token");
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

        return appUserMapper.toProfileDto(user);
    }

    private JwtAccessTokenDTO buildJwtToken(AppUser user) {
        String token = jwtConfig.generateToken(user.getUsername(), user.getId(), user.getRole().name());
        return JwtAccessTokenDTO.builder()
                .accessToken(token)
                .role(user.getRole().name())
                .build();
    }

    private AppUserDTO mapToDTO(AppUser user) {
        return appUserMapper.toUserDto(user);
    }

    private PricingPlanRef resolvePaidPlan(String planCode) {
        if (planCode == null || "FREE_TRIAL".equals(planCode)) return null;
        return pricingPlanRepository.findByCodeAndActiveTrue(planCode)
                .orElseThrow(() -> new IllegalArgumentException("Selected pricing plan is not available"));
    }

    private void createInitialSubscription(AppUser user, PricingPlanRef plan) {
        LocalDate today = LocalDate.now();
        UserSubscription sub = UserSubscription.builder().user(user).pricingPlanId(plan.getId())
                .planCodeSnapshot(plan.getCode()).planNameSnapshot(plan.getDisplayName()).priceMinorSnapshot(plan.getPriceMinor()).currencySnapshot(plan.getCurrency()).featuresSnapshot(featuresJson(plan))
                .status("ACTIVE").startDate(today).endDate(plan.getDurationDays() == null ? null : today.plusDays(plan.getDurationDays())).durationDays(plan.getDurationDays()).build();
        user.getSubscriptions().add(sub); appUserRepository.save(user);
    }

    private String featuresJson(PricingPlanRef plan) {
        return plan.getFeatures().stream().map(feature -> "\"" + feature.getFeatureText().replace("\\", "\\\\").replace("\"", "\\\"") + "\"").collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }
}
