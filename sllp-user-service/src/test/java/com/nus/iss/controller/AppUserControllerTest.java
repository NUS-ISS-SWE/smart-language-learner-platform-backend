package com.nus.iss.controller;

import com.nus.iss.dto.AppUserDTO;
import com.nus.iss.dto.JwtAccessTokenDTO;
import com.nus.iss.dto.ResetPasswordConfirmDTO;
import com.nus.iss.dto.UserProfileDTO;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.service.AppUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserControllerTest {

    @Mock
    private AppUserService appUserService;

    @Test
    void registerReturnsRegisteredUser() {
        AppUserController controller = new AppUserController(appUserService);
        AppUserDTO request = AppUserDTO.builder()
                .username("ming")
                .email("ming@example.com")
                .password("password123")
                .build();
        AppUserDTO registeredUser = AppUserDTO.builder()
                .id(1L)
                .username("ming")
                .email("ming@example.com")
                .build();
        when(appUserService.registerUser(request)).thenReturn(registeredUser);

        ResponseEntity<AppUserDTO> response = controller.register(request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(registeredUser, response.getBody());
    }

    @Test
    void activateDelegatesToServiceAndReturnsOk() {
        AppUserController controller = new AppUserController(appUserService);

        ResponseEntity<Void> response = controller.activate("activation-token");

        assertEquals(200, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(appUserService).activateUser("activation-token");
    }

    @Test
    void loginReturnsJwtAccessToken() {
        AppUserController controller = new AppUserController(appUserService);
        AppUserDTO request = AppUserDTO.builder()
                .username("ming")
                .password("password123")
                .build();
        JwtAccessTokenDTO accessToken = JwtAccessTokenDTO.builder()
                .accessToken("jwt-token")
                .role("USER_FREE_TRIAL")
                .build();
        when(appUserService.login("ming", "password123")).thenReturn(accessToken);

        ResponseEntity<JwtAccessTokenDTO> response = controller.login(request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(accessToken, response.getBody());
    }

    @Test
    void forgotPasswordDelegatesToServiceAndReturnsOk() {
        AppUserController controller = new AppUserController(appUserService);
        AppUserDTO request = AppUserDTO.builder()
                .email("ming@example.com")
                .build();

        ResponseEntity<Void> response = controller.forgotPassword(request);

        assertEquals(200, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(appUserService).forgotPassword("ming@example.com");
    }

    @Test
    void confirmForgotPasswordReturnsNewAccessToken() {
        AppUserController controller = new AppUserController(appUserService);
        ResetPasswordConfirmDTO request = new ResetPasswordConfirmDTO("reset-token", "new-password");
        JwtAccessTokenDTO token = JwtAccessTokenDTO.builder().accessToken("jwt-token").role("USER_PAID").build();
        when(appUserService.confirmForgotPassword("reset-token", "new-password")).thenReturn(token);

        ResponseEntity<JwtAccessTokenDTO> response = controller.confirmForgotPassword(request);

        assertSame(token, response.getBody());
        verify(appUserService).confirmForgotPassword("reset-token", "new-password");
    }

    @Test
    void getUserProfileReturnsInternalProfile() {
        AppUserController controller = new AppUserController(appUserService);
        UserProfileDTO profile = UserProfileDTO.builder()
                .id(1L)
                .username("ming")
                .difficultyLevel(DifficultyLevel.INTERMEDIATE)
                .build();
        when(appUserService.getUserProfile(1L)).thenReturn(profile);

        ResponseEntity<UserProfileDTO> response = controller.getUserProfile(1L);

        assertEquals(200, response.getStatusCode().value());
        assertSame(profile, response.getBody());
        verify(appUserService).getUserProfile(1L);
    }

    @Test
    void resetPasswordDelegatesToServiceAndReturnsOk() {
        AppUserController controller = new AppUserController(appUserService);
        AppUserDTO request = AppUserDTO.builder()
                .username("ming")
                .password("old-password")
                .newPassword("new-password")
                .build();

        ResponseEntity<Void> response = controller.resetPassword("ming", "USER_FREE_TRIAL", request);

        assertEquals(200, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(appUserService).resetPassword(request);
    }
}
