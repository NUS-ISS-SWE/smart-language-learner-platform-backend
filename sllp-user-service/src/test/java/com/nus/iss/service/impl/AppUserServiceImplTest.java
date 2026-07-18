package com.nus.iss.service.impl;

import com.nus.iss.config.JwtConfig;
import com.nus.iss.dto.AppUserDTO;
import com.nus.iss.dto.JwtAccessTokenDTO;
import com.nus.iss.entity.AppUser;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.PreferredLanguage;
import com.nus.iss.enums.Role;
import com.nus.iss.enums.Status;
import com.nus.iss.exception.UserAlreadyExistsException;
import com.nus.iss.repository.AppUserRepository;
import com.nus.iss.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserServiceImplTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private AppUserServiceImpl appUserService;

    @Test
    void registerUser_whenAccountIsNewSavesDefaultUserAndSendsActivationEmail() {
        AppUserDTO request = AppUserDTO.builder()
                .username("ming")
                .email("ming@example.com")
                .password("password123")
                .build();
        when(appUserRepository.existsByUsername("ming")).thenReturn(false);
        when(appUserRepository.existsByEmail("ming@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser savedUser = invocation.getArgument(0);
            savedUser.setId(1L);
            return savedUser;
        });

        AppUserDTO response = appUserService.registerUser(request);

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());
        AppUser savedUser = userCaptor.getValue();
        verify(notificationService).sendActivationEmail(savedUser);

        assertEquals(1L, response.getId());
        assertEquals("ming", response.getUsername());
        assertEquals("ming@example.com", response.getEmail());
        assertNull(response.getPassword());
        assertEquals(Status.PENDING_EMAIL_VERIFICATION, response.getStatus());
        assertEquals(Role.USER_FREE_TRIAL, response.getRole());
        assertEquals(PreferredLanguage.ENGLISH, response.getPreferredLanguage());
        assertEquals(DifficultyLevel.BEGINNER, response.getDifficultyLevel());
        assertEquals(5000L, response.getToken());
        assertEquals(0L, response.getTotalTokenConsumption());

        assertEquals("encoded-password", savedUser.getPassword());
        assertEquals(Status.PENDING_EMAIL_VERIFICATION, savedUser.getStatus());
        assertEquals(Role.USER_FREE_TRIAL, savedUser.getRole());
        assertEquals(PreferredLanguage.ENGLISH, savedUser.getPreferredLanguage());
        assertEquals(DifficultyLevel.BEGINNER, savedUser.getDifficultyLevel());
        assertNotNull(savedUser.getCreatedAt());
    }

    @Test
    void registerUser_whenLanguageAndDifficultyAreProvidedKeepsRequestedValues() {
        AppUserDTO request = AppUserDTO.builder()
                .username("ming")
                .email("ming@example.com")
                .password("password123")
                .preferredLanguage(PreferredLanguage.CHINESE)
                .difficultyLevel(DifficultyLevel.ADVANCED)
                .build();
        when(appUserRepository.existsByUsername("ming")).thenReturn(false);
        when(appUserRepository.existsByEmail("ming@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUserDTO response = appUserService.registerUser(request);

        assertEquals(PreferredLanguage.CHINESE, response.getPreferredLanguage());
        assertEquals(DifficultyLevel.ADVANCED, response.getDifficultyLevel());
    }

    @Test
    void registerUser_whenUsernameExistsThrowsUserAlreadyExistsException() {
        AppUserDTO request = AppUserDTO.builder()
                .username("ming")
                .email("ming@example.com")
                .password("password123")
                .build();
        when(appUserRepository.existsByUsername("ming")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> appUserService.registerUser(request));

        verify(appUserRepository, never()).save(any(AppUser.class));
        verifyNoInteractions(passwordEncoder, notificationService, jwtConfig);
    }

    @Test
    void registerUser_whenEmailExistsThrowsUserAlreadyExistsException() {
        AppUserDTO request = AppUserDTO.builder()
                .username("ming")
                .email("ming@example.com")
                .password("password123")
                .build();
        when(appUserRepository.existsByUsername("ming")).thenReturn(false);
        when(appUserRepository.existsByEmail("ming@example.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> appUserService.registerUser(request));

        verify(appUserRepository, never()).save(any(AppUser.class));
        verifyNoInteractions(passwordEncoder, notificationService, jwtConfig);
    }

    @Test
    void registerUser_whenActivationEmailFailsStillReturnsRegisteredUser() {
        AppUserDTO request = AppUserDTO.builder()
                .username("ming")
                .email("ming@example.com")
                .password("password123")
                .build();
        when(appUserRepository.existsByUsername("ming")).thenReturn(false);
        when(appUserRepository.existsByEmail("ming@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("mail down")).when(notificationService).sendActivationEmail(any(AppUser.class));

        AppUserDTO response = assertDoesNotThrow(() -> appUserService.registerUser(request));

        assertEquals("ming", response.getUsername());
        verify(notificationService).sendActivationEmail(any(AppUser.class));
    }

    @Test
    void login_whenCredentialsAreValidReturnsJwtAccessToken() {
        AppUser user = activeUser();
        when(appUserRepository.findByUsername("ming")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtConfig.generateToken("ming", "USER_FREE_TRIAL")).thenReturn("jwt-token");

        JwtAccessTokenDTO response = appUserService.login("ming", "password123");

        assertEquals("jwt-token", response.getAccessToken());
        assertEquals("USER_FREE_TRIAL", response.getRole());
        verify(jwtConfig).generateToken("ming", "USER_FREE_TRIAL");
    }

    @Test
    void login_whenUserDoesNotExistThrowsBadCredentialsException() {
        when(appUserRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> appUserService.login("missing", "password123"));

        verifyNoInteractions(passwordEncoder, jwtConfig);
    }

    @Test
    void login_whenUserIsNotActiveThrowsBadCredentialsException() {
        AppUser user = activeUser();
        user.setStatus(Status.PENDING_EMAIL_VERIFICATION);
        when(appUserRepository.findByUsername("ming")).thenReturn(Optional.of(user));

        assertThrows(BadCredentialsException.class, () -> appUserService.login("ming", "password123"));

        verifyNoInteractions(passwordEncoder, jwtConfig);
    }

    @Test
    void login_whenPasswordDoesNotMatchThrowsBadCredentialsException() {
        AppUser user = activeUser();
        when(appUserRepository.findByUsername("ming")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> appUserService.login("ming", "wrong-password"));

        verifyNoInteractions(jwtConfig);
    }

    @Test
    void forgotPassword_whenEmailExistsSendsForgotPasswordEmail() {
        AppUser user = activeUser();
        when(appUserRepository.findByEmail("ming@example.com")).thenReturn(Optional.of(user));

        appUserService.forgotPassword("ming@example.com");

        verify(notificationService).sendForgotPasswordEmail(user);
    }

    @Test
    void forgotPassword_whenEmailDoesNotExistThrowsBadCredentialsException() {
        when(appUserRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> appUserService.forgotPassword("missing@example.com"));

        verifyNoInteractions(notificationService);
    }

    @Test
    void resetPassword_whenOldPasswordMatchesSavesEncodedNewPassword() {
        AppUser user = activeUser();
        AppUserDTO request = AppUserDTO.builder()
                .username("ming")
                .password("old-password")
                .newPassword("new-password")
                .build();
        when(appUserRepository.findByUsername("ming")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "encoded-password")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new-password");

        appUserService.resetPassword(request);

        assertEquals("encoded-new-password", user.getPassword());
        verify(appUserRepository).save(user);
    }

    @Test
    void resetPassword_whenUserDoesNotExistThrowsBadCredentialsException() {
        AppUserDTO request = AppUserDTO.builder()
                .username("missing")
                .password("old-password")
                .newPassword("new-password")
                .build();
        when(appUserRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> appUserService.resetPassword(request));

        verifyNoInteractions(passwordEncoder);
        verify(appUserRepository, never()).save(any(AppUser.class));
    }

    @Test
    void resetPassword_whenOldPasswordDoesNotMatchThrowsBadCredentialsException() {
        AppUser user = activeUser();
        AppUserDTO request = AppUserDTO.builder()
                .username("ming")
                .password("wrong-password")
                .newPassword("new-password")
                .build();
        when(appUserRepository.findByUsername("ming")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> appUserService.resetPassword(request));

        verify(appUserRepository, never()).save(any(AppUser.class));
    }

    @Test
    void activateUser_whenTokenMatchesPendingUserActivatesAndSavesUser() {
        AppUser user = activeUser();
        user.setStatus(Status.PENDING_EMAIL_VERIFICATION);
        String token = activationToken("ming@example.com");
        when(appUserRepository.findByEmail("ming@example.com")).thenReturn(Optional.of(user));

        appUserService.activateUser(token);

        assertEquals(Status.ACTIVE, user.getStatus());
        verify(appUserRepository).save(user);
    }

    @Test
    void activateUser_whenTokenIsNotBase64ThrowsBadCredentialsException() {
        assertThrows(BadCredentialsException.class, () -> appUserService.activateUser("not-base64-token"));

        verify(appUserRepository, never()).findByEmail(any());
        verify(appUserRepository, never()).save(any(AppUser.class));
    }

    @Test
    void activateUser_whenTokenDoesNotMatchUserThrowsBadCredentialsException() {
        String token = activationToken("missing@example.com");
        when(appUserRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> appUserService.activateUser(token));

        verify(appUserRepository, never()).save(any(AppUser.class));
    }

    @Test
    void activateUser_whenUserAlreadyActiveDoesNotSaveUser() {
        AppUser user = activeUser();
        String token = activationToken("ming@example.com");
        when(appUserRepository.findByEmail("ming@example.com")).thenReturn(Optional.of(user));

        appUserService.activateUser(token);

        assertEquals(Status.ACTIVE, user.getStatus());
        verify(appUserRepository, never()).save(any(AppUser.class));
    }

    private static AppUser activeUser() {
        return AppUser.builder()
                .id(1L)
                .username("ming")
                .email("ming@example.com")
                .password("encoded-password")
                .status(Status.ACTIVE)
                .role(Role.USER_FREE_TRIAL)
                .preferredLanguage(PreferredLanguage.ENGLISH)
                .difficultyLevel(DifficultyLevel.BEGINNER)
                .token(5000L)
                .totalTokenConsumption(0L)
                .build();
    }

    private static String activationToken(String email) {
        return Base64.getEncoder().encodeToString(email.getBytes(StandardCharsets.UTF_8));
    }
}
