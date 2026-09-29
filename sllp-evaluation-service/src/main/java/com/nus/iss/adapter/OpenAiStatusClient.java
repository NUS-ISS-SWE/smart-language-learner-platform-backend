package com.nus.iss.adapter;

import com.nus.iss.dto.OpenAiStatusResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
public class OpenAiStatusClient {
    private final RestClient restClient;
    @Getter
    private volatile boolean operational;

    public OpenAiStatusClient(@Qualifier("openAiStatusRestClient") RestClient openAiStatusRestClient) {
        this.restClient = openAiStatusRestClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeStatus() {
        try {
            var response = restClient.get().retrieve().toEntity(OpenAiStatusResponse.class);
            OpenAiStatusResponse body = response.getBody();
            operational = response.getStatusCode().value() == 200 && body != null && body.summary() != null
                    && body.summary().affectedComponents() != null && body.summary().affectedComponents().isEmpty()
                    && body.summary().ongoingIncidents() != null && body.summary().ongoingIncidents().isEmpty();
            log.info("OpenAI startup status check completed operational={}", operational);
        } catch (RestClientException exception) {
            operational = false;
            log.warn("OpenAI startup status check failed: {}", exception.getMessage());
        }
    }
}
