package com.nus.iss.client;

import com.nus.iss.dto.UserProfileDTO;

public interface UserProfileClient {
    UserProfileDTO getUserProfile(Long userId);
}
