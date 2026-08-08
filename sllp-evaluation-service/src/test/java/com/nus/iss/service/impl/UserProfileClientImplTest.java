package com.nus.iss.service.impl;

import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.UserProfileDTO;
import com.nus.iss.enums.DifficultyLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class UserProfileClientImplTest {

    private MockRestServiceServer server;
    private UserProfileClientImpl userProfileClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();

        SllpProperties properties = new SllpProperties();
        properties.getServices().setUserServiceUrl("http://user-service:8082");
        userProfileClient = new UserProfileClientImpl(restClientBuilder, properties);
    }

    @Test
    void getUserProfileUsesUserIdAndReturnsDifficultyLevel() {
        server.expect(requestTo("http://user-service:8082/api/v1/auth/profile/1"))
                .andRespond(withSuccess(
                        "{\"id\":1,\"username\":\"ming\",\"difficultyLevel\":\"ADVANCED\"}",
                        MediaType.APPLICATION_JSON));

        UserProfileDTO profile = userProfileClient.getUserProfile(1L);

        assertEquals(1L, profile.id());
        assertEquals("ming", profile.username());
        assertEquals(DifficultyLevel.ADVANCED, profile.difficultyLevel());
        server.verify();
    }
}
