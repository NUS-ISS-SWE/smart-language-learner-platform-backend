package com.nus.iss.controller;

import com.nus.iss.adapter.OpenAiStatusClient;
import com.nus.iss.config.SllpProperties;
import com.nus.iss.repository.GradingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentMetadataControllerUnitTest {

    @Mock
    private OpenAiStatusClient statusClient;
    @Mock
    private GradingRepository gradingRepository;

    private SllpProperties properties;
    private AgentMetadataController controller;

    @BeforeEach
    void setUp() {
        properties = new SllpProperties();
        properties.setPublicBaseUrl("https://grading.example.com");
        controller = new AgentMetadataController(properties, statusClient, gradingRepository);
    }

    @Test
    void cardContainsConfiguredUrlAndSkillMetadata() {
        Map<String, Object> card = controller.card();

        assertEquals("SLLP Grading Agent", card.get("name"));
        assertEquals("https://grading.example.com/a2a", card.get("url"));
        assertEquals("1.0", card.get("protocolVersion"));
    }

    @Test
    void healthIsReadyOnlyWhenOpenAiAndDatabaseAreOperational() {
        when(statusClient.isOperational()).thenReturn(true);
        when(gradingRepository.count()).thenReturn(4L);

        ResponseEntity<Map<String, String>> response = controller.health();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("ready", response.getBody().get("status"));
        verify(gradingRepository).count();
    }

    @Test
    void healthIsUnavailableWhenDatabaseThrows() {
        when(statusClient.isOperational()).thenReturn(true);
        when(gradingRepository.count()).thenThrow(new IllegalStateException("database offline"));

        ResponseEntity<Map<String, String>> response = controller.health();

        assertEquals(503, response.getStatusCode().value());
        assertEquals("unavailable", response.getBody().get("status"));
    }

    @Test
    void livenessIsPureAndAlwaysAlive() {
        assertEquals(Map.of("status", "alive"), controller.live());
    }
}
