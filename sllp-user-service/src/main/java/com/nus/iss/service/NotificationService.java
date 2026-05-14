package com.nus.iss.service;

import com.nus.iss.entity.AppUser;

public interface NotificationService {
    void sendActivationEmail(AppUser savedAppUser);

    void sendDeletionEmail(AppUser appUser);

    void sendForgotPasswordEmail(AppUser user);
}
