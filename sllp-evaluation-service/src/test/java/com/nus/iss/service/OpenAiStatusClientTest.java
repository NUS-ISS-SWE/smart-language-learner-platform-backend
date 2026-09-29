package com.nus.iss.service;

import com.nus.iss.adapter.OpenAiStatusClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiStatusClientTest {
    private MockRestServiceServer server;
    private OpenAiStatusClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://status.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OpenAiStatusClient(builder.build());
    }

    @Test
    void reportsOperationalForAHealthyStatusPayload() {
        expectStatus("""
                {"summary":{"affected_components":[],"ongoing_incidents":[]}}
                """);

        assertTrue(client.isOperational());
        server.verify();
    }

    @Test
    void reportsUnavailableWhenAComponentIsAffected() {
        expectStatus("""
                {"summary":{"affected_components":[{"name":"Chat Completions"}],"ongoing_incidents":[]}}
                """);

        assertFalse(client.isOperational());
        server.verify();
    }

    @Test
    void reportsUnavailableWhenAnIncidentIsOngoing() {
        expectStatus("""
                {"summary":{"affected_components":[],"ongoing_incidents":[{"name":"API errors"}]}}
                """);

        assertFalse(client.isOperational());
        server.verify();
    }

    @Test
    void reportsUnavailableForAnUnexpectedPayload() {
        expectStatus("""
                {"summary":{"affected_components":[]}}
                """);

        assertFalse(client.isOperational());
        server.verify();
    }

    @Test
    void reportsUnavailableWhenTheStatusEndpointFails() {
        server.expect(once(), requestTo("https://status.test"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertFalse(client.isOperational());
        server.verify();
    }

    private void expectStatus(String body) {
        server.expect(once(), requestTo("https://status.test"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }
}
