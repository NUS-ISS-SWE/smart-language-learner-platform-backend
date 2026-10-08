package com.nus.iss.adapter;

import com.nus.iss.config.SllpProperties;
import com.nus.iss.enums.DifficultyLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SllpUserClientTest {
    private MockRestServiceServer server;
    private SllpUserClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        SllpProperties properties = new SllpProperties();
        properties.getServices().setUserServiceUrl("https://users.test");
        client = new SllpUserClient(builder, properties);
    }

    @Test
    void returnsDifficultyLevelFromUserProfile() {
        expectProfile(7L, "INTERMEDIATE");

        assertEquals(DifficultyLevel.INTERMEDIATE, client.getDifficultyLevel(7L));
        server.verify();
    }

    @Test
    void rejectsProfileWithoutDifficultyLevel() {
        server.expect(once(), requestTo("https://users.test/api/v1/auth/profile/7"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":7,\"username\":\"learner\"}", MediaType.APPLICATION_JSON));

        assertThrows(IllegalStateException.class, () -> client.getDifficultyLevel(7L));
        server.verify();
    }

    @Test
    void wrapsUserServiceFailure() {
        server.expect(once(), requestTo("https://users.test/api/v1/auth/profile/7"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThrows(IllegalStateException.class, () -> client.getDifficultyLevel(7L));
        server.verify();
    }

    private void expectProfile(long userId, String difficultyLevel) {
        server.expect(once(), requestTo("https://users.test/api/v1/auth/profile/" + userId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":%d,"username":"learner","difficultyLevel":"%s"}
                        """.formatted(userId, difficultyLevel), MediaType.APPLICATION_JSON));
    }
}
