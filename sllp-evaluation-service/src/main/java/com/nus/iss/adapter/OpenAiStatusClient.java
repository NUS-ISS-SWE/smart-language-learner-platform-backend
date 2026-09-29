package com.nus.iss.adapter;

import com.nus.iss.dto.OpenAiStatusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
public class OpenAiStatusClient {
    private final RestClient restClient;

    public OpenAiStatusClient(@Qualifier("openAiStatusRestClient") RestClient openAiStatusRestClient) {
        this.restClient = openAiStatusRestClient;
    }

    public boolean isOperational() {
        try {
            var response = restClient.get().retrieve().toEntity(OpenAiStatusResponse.class);
            OpenAiStatusResponse body = response.getBody();
            return response.getStatusCode().value() == 200 && body != null && body.summary() != null
                    && body.summary().affectedComponents() != null && body.summary().affectedComponents().isEmpty()
                    && body.summary().ongoingIncidents() != null && body.summary().ongoingIncidents().isEmpty();
        } catch (RestClientException exception) {
            log.warn("OpenAI status check failed: {}", exception.getMessage());
            return false;
        }
    }
}
