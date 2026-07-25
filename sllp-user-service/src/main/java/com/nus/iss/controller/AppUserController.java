package com.nus.iss.controller;

import com.nus.iss.dto.AppUserDTO;
import com.nus.iss.dto.JwtAccessTokenDTO;
import com.nus.iss.dto.ResetPasswordConfirmDTO;
import com.nus.iss.service.AppUserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AppUserController {
    private final AppUserService appUserService;

    @Autowired
    public AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @PostMapping("/register")
    public ResponseEntity<AppUserDTO> register(@Valid @RequestBody AppUserDTO appUserDTO) {
        log.info("Registering user: {}", appUserDTO);
        AppUserDTO registeredUser = appUserService.registerUser(appUserDTO);
        return ResponseEntity.ok(registeredUser);
    }

    @GetMapping("/activate")
    public ResponseEntity<Void> activate(@RequestParam String token) {
        log.info("Activating user with token: {}", token);
        appUserService.activateUser(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<JwtAccessTokenDTO> login(@RequestBody AppUserDTO appUserDTO) {
        log.info("Login attempt for user: {}", appUserDTO.getUsername());
        JwtAccessTokenDTO jwtAccessTokenDTO = appUserService.login(appUserDTO.getUsername(), appUserDTO.getPassword());
        return ResponseEntity.ok(jwtAccessTokenDTO);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody AppUserDTO appUserDTO) {
        log.info("Forgot password request for email: {}", appUserDTO.getEmail());
        appUserService.forgotPassword(appUserDTO.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password/confirm")
    public ResponseEntity<JwtAccessTokenDTO> confirmForgotPassword(@Valid @RequestBody ResetPasswordConfirmDTO dto) {
        log.info("Confirm forgot password request");
        JwtAccessTokenDTO jwtAccessToken = appUserService.confirmForgotPassword(dto.token(), dto.newPassword());
        return ResponseEntity.ok(jwtAccessToken);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestHeader("X-Sllp-Username") String username,
                                              @RequestHeader("X-Sllp-Role") String role,
                                              @RequestBody AppUserDTO appUserDTO) {
        log.info("Reset password request for user: {}", appUserDTO.getUsername());
        appUserService.resetPassword(appUserDTO);
        return ResponseEntity.ok().build();
    }


}
