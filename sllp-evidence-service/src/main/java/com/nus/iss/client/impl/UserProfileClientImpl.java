package com.nus.iss.client.impl;

import com.nus.iss.agent.FallbackPolicy;
import com.nus.iss.client.UserProfileClient;
import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.UserProfileDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
        try {
            UserProfileDTO profile = restClient.get()
                    .uri("/api/v1/auth/profile/{userId}", userId)
                    .retrieve()
                    .body(UserProfileDTO.class);

            if (profile == null || profile.difficultyLevel() == null) {
                throw FallbackPolicy.upstreamUnavailable("user-service (no profile for user " + userId + ")", null);
            }
            return profile;
        } catch (RestClientException ex) {
            throw FallbackPolicy.upstreamUnavailable("user-service", ex);
        }
    }
}
