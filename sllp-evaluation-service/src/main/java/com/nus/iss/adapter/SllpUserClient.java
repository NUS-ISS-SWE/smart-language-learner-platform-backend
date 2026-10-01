package com.nus.iss.adapter;

import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.UserProfileDTO;
import com.nus.iss.enums.DifficultyLevel;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class SllpUserClient {
    private final RestClient restClient;

    public SllpUserClient(RestClient.Builder restClientBuilder, SllpProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.getServices().getUserServiceUrl())
                .build();
    }

    public DifficultyLevel getDifficultyLevel(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }

        try {
            UserProfileDTO profile = restClient.get()
                    .uri("/api/v1/auth/profile/{userId}", userId)
                    .retrieve()
                    .body(UserProfileDTO.class);

            if (profile == null || profile.difficultyLevel() == null) {
                throw new IllegalStateException("user-service returned no difficulty level for user " + userId);
            }
            return profile.difficultyLevel();
        } catch (RestClientException exception) {
            throw new IllegalStateException("could not retrieve difficulty level from user-service", exception);
        }
    }
}
