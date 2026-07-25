package com.nus.iss.service;

import com.nus.iss.dto.AppUserDTO;
import com.nus.iss.dto.JwtAccessTokenDTO;

public interface AppUserService {
    AppUserDTO registerUser(AppUserDTO appUserDTO);

    JwtAccessTokenDTO login(String username, String password);

    void forgotPassword(String email);

    JwtAccessTokenDTO confirmForgotPassword(String token, String newPassword);

    void resetPassword(AppUserDTO appUserDTO);

    void activateUser(String token);
}
