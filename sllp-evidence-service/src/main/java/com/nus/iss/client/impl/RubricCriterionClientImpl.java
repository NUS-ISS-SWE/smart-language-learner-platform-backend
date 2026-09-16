package com.nus.iss.client.impl;

import com.nus.iss.agent.FallbackPolicy;
import com.nus.iss.client.RubricCriterionClient;
import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.RubricCriterionDTO;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
public class RubricCriterionClientImpl implements RubricCriterionClient {

    private final RestClient restClient;

    public RubricCriterionClientImpl(RestClient.Builder restClientBuilder, SllpProperties sllpProperties) {
        this.restClient = restClientBuilder
                .baseUrl(sllpProperties.getServices().getAdminServiceUrl())
                .build();
    }

    @Override
    public List<RubricCriterionDTO> getRubricCriteria(Language language, DifficultyLevel difficultyLevel) {
        try {
            List<RubricCriterionDTO> criteria = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/admin/rubrics")
                            .queryParam("language", language.name())
                            .queryParam("difficultyLevel", difficultyLevel.name())
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (criteria == null || criteria.isEmpty()) {
                throw FallbackPolicy.upstreamUnavailable("admin-service (no rubric for "
                        + language + " " + difficultyLevel + ")", null);
            }
            return criteria;
        } catch (RestClientException ex) {
            throw FallbackPolicy.upstreamUnavailable("admin-service", ex);
        }
    }
}
