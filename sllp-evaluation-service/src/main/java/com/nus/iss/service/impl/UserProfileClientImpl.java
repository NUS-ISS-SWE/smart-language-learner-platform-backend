package com.nus.iss.service.impl;

import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.UserProfileDTO;
import com.nus.iss.service.UserProfileClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class UserProfileClientImpl implements UserProfileClient {

    private final RestClient restClient;

    public UserProfileClientImpl(RestClient.Builder restClientBuilder, SllpProperties sllpProperties) {
        this.restClient = restClientBuilder
                .baseUrl(sllpProperties.getServices().getUserServiceUrl())
                .build();
    }

    @Override
    public UserProfileDTO getUserProfile(Long userId) {
        UserProfileDTO profile = restClient.get()
                .uri("/api/v1/auth/profile/{userId}", userId)
                .retrieve()
                .body(UserProfileDTO.class);

        if (profile == null) {
            throw new IllegalStateException("User Service returned no profile for user ID: " + userId);
        }
        if (profile.difficultyLevel() == null) {
            throw new IllegalStateException("User profile has no difficulty level for user ID: " + userId);
        }
        return profile;
    }
}
