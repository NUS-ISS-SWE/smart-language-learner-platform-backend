package com.nus.iss.client.impl;

import com.nus.iss.agent.FallbackPolicy;
import com.nus.iss.client.CompositionClient;
import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.CompositionDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class CompositionClientImpl implements CompositionClient {

    private final RestClient restClient;

    public CompositionClientImpl(RestClient.Builder restClientBuilder, SllpProperties sllpProperties) {
        this.restClient = restClientBuilder
                .baseUrl(sllpProperties.getServices().getCompositionServiceUrl())
                .build();
    }

    @Override
    public CompositionDTO getComposition(Long userId, Long compositionId) {
        try {
            CompositionDTO composition = restClient.get()
                    .uri("/api/v1/composition/{id}", compositionId)
                    .header("X-Sllp-User-Id", String.valueOf(userId))
                    .retrieve()
                    .body(CompositionDTO.class);

            if (composition == null) {
                throw FallbackPolicy.upstreamUnavailable("composition-service", null);
            }
            return composition;
        } catch (RestClientException ex) {
            throw FallbackPolicy.upstreamUnavailable("composition-service", ex);
        }
    }
}
