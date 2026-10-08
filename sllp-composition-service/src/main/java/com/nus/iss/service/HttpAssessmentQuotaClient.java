package com.nus.iss.service;

import com.nus.iss.exception.AssessmentQuotaExceededException;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class HttpAssessmentQuotaClient implements AssessmentQuotaClient {
    private final RestClient client;
    private final String token;

    public HttpAssessmentQuotaClient(RestClient.Builder builder,
                                     @Value("${sllp.subscription.base-url:http://localhost:8084}") String baseUrl,
                                     @Value("${sllp.internal-service-token:}") String token) {
        this.client = builder.baseUrl(baseUrl).build();
        this.token = token;
    }

    @Override
    public void reserve(Long userId, UUID requestId) {
        try {
            client.post().uri("/internal/assessment-quotas/reservations")
                    .header("X-Sllp-Internal-Token", token)
                    .body(Map.of("userId", userId, "requestId", requestId.toString()))
                    .retrieve().toBodilessEntity();
        } catch (RestClientResponseException error) {
            if (error.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) throw new AssessmentQuotaExceededException();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "assessment quota service is unavailable");
        } catch (RestClientException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "assessment quota service is unavailable");
        }
    }

    @Override
    public void settle(UUID requestId, boolean consumed) {
        try {
            client.post().uri("/internal/assessment-quotas/reservations/{requestId}/settlement?consumed={consumed}", requestId, consumed)
                    .header("X-Sllp-Internal-Token", token).retrieve().toBodilessEntity();
        } catch (RestClientResponseException error) {
            throw new IllegalStateException("assessment quota settlement is unavailable");
        }
    }
}
