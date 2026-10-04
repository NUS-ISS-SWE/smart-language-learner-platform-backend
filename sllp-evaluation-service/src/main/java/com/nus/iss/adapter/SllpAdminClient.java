package com.nus.iss.adapter;

import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.AdminRubricCriterionDTO;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class SllpAdminClient {
    private final RestClient restClient;

    public SllpAdminClient(RestClient.Builder restClientBuilder, SllpProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.getServices().getAdminServiceUrl())
                .build();
    }

    public List<AdminRubricCriterionDTO> getRubricCriteria(
            Language language, DifficultyLevel difficultyLevel) {
        try {
            List<AdminRubricCriterionDTO> criteria = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/admin/rubrics")
                            .queryParam("language", language.name())
                            .queryParam("difficultyLevel", difficultyLevel.name())
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (criteria == null || criteria.isEmpty()) {
                throw new IllegalStateException("admin-service returned no matching rubric criteria");
            }
            return criteria;
        } catch (RestClientException exception) {
            throw new IllegalStateException("could not retrieve rubric criteria from admin-service", exception);
        }
    }
}
