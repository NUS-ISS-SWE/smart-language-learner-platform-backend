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
    void inputSchemaDescribesRequiredCompositionGradingFields() {
        Map<String, Object> schema = controller.inputSchema();

        assertEquals("object", schema.get("type"));
        assertEquals(
                java.util.List.of("schemaVersion", "workflowId", "attempt", "composition", "evidence", "rubricSnapshot"),
                schema.get("required"));
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertEquals("object", ((Map<?, ?>) properties.get("composition")).get("type"));
        assertEquals("object", ((Map<?, ?>) properties.get("rubricSnapshot")).get("type"));
    }

    @Test
    void outputSchemaConstrainsScoreConfidenceAndCriteria() {
        Map<String, Object> schema = controller.outputSchema();

        assertEquals("object", schema.get("type"));
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertEquals(100, ((Map<?, ?>) properties.get("overallScore")).get("maximum"));
        assertEquals(1, ((Map<?, ?>) properties.get("confidence")).get("maximum"));
        assertEquals(1, ((Map<?, ?>) properties.get("criteria")).get("minItems"));
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
