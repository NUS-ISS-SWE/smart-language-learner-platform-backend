package com.nus.iss.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class OpenAiStatusClientConfigTest {

    @Test
    void buildsRestClientFromConfiguredHealthSettingsWithoutNetworkCall() {
        SllpProperties properties = new SllpProperties();
        properties.getHealth().setOpenAiStatusUrl(URI.create("https://status.example.test/api"));
        properties.getHealth().setConnectTimeout(Duration.ofMillis(250));
        properties.getHealth().setReadTimeout(Duration.ofMillis(500));

        RestClient client = new OpenAiStatusClientConfig()
                .openAiStatusRestClient(RestClient.builder(), properties);

        assertNotNull(client);
    }
}
