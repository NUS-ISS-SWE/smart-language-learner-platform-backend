package com.nus.iss.service.impl;

import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.UserProfileDTO;
import com.nus.iss.service.UserProfileClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class UserProfileClientImpl implements UserProfileClient {

    private final RestTemplate restTemplate;
    private final String userServiceBaseUrl;

    public UserProfileClientImpl(RestTemplate restTemplate, SllpProperties sllpProperties) {
        this.restTemplate = restTemplate;
        this.userServiceBaseUrl = sllpProperties.getServices() != null
                ? sllpProperties.getServices().getUserServiceUrl()
                : null;
    }

    @Override
    public UserProfileDTO getUserProfile(Long userId) {
        if (userServiceBaseUrl == null || userServiceBaseUrl.isBlank()) {
            throw new IllegalStateException("User service URL is not configured");
        }

        String url = userServiceBaseUrl + "/api/v1/auth/profile/" + userId;
        ResponseEntity<UserProfileDTO> response = restTemplate.getForEntity(url, UserProfileDTO.class);
        UserProfileDTO profile = response.getBody();

        if (profile == null) {
            throw new IllegalStateException("User Service returned no profile for user ID: " + userId);
        }
        if (profile.getDifficultyLevel() == null) {
            throw new IllegalStateException("User profile has no difficulty level for user ID: " + userId);
        }
        return profile;
    }
}
