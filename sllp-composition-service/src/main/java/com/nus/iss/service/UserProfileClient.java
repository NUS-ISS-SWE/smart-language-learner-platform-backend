package com.nus.iss.service;

import com.nus.iss.dto.UserProfileDTO;

public interface UserProfileClient {
    UserProfileDTO getUserProfile(Long userId);
}
