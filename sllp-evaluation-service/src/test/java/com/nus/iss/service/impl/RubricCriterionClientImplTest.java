package com.nus.iss.service.impl;

import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.RubricCriterionDTO;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RubricCriterionClientImplTest {

    private MockRestServiceServer server;
    private RubricCriterionClientImpl rubricCriterionClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();

        SllpProperties properties = new SllpProperties();
        properties.getServices().setAdminServiceUrl("http://admin-service:8086");
        rubricCriterionClient = new RubricCriterionClientImpl(restClientBuilder, properties);
    }

    @Test
    void getsRubricCriteriaByCompositionLanguageAndUserDifficultyLevel() {
        server.expect(requestTo("http://admin-service:8086/api/v1/admin/rubrics"
                        + "?language=CHINESE&difficultyLevel=ADVANCED"))
                .andRespond(withSuccess("""
                        [{
                          "id": 7,
                          "language": "CHINESE",
                          "difficultyLevel": "ADVANCED",
                          "criterionName": "CONTENT_RELEVANCE",
                          "criterionDescription": "Assess relevant and well-developed content.",
                          "weightPercentage": 25.00,
                          "createdBy": "SYSTEM",
                          "updatedBy": "SYSTEM",
                          "createdAt": "2026-08-04T10:00:00",
                          "updatedAt": "2026-08-04T10:00:00"
                        }]
                        """, MediaType.APPLICATION_JSON));

        List<RubricCriterionDTO> criteria = rubricCriterionClient.getRubricCriteria(
                Language.CHINESE, DifficultyLevel.ADVANCED);

        assertEquals(1, criteria.size());
        assertEquals("CONTENT_RELEVANCE", criteria.getFirst().criterionName());
        assertEquals(new BigDecimal("25.00"), criteria.getFirst().weightPercentage());
        assertEquals("SYSTEM", criteria.getFirst().createdBy());
        server.verify();
    }

    @Test
    void failsWhenAdminServiceReturnsNoRubricCriteria() {
        server.expect(requestTo("http://admin-service:8086/api/v1/admin/rubrics"
                        + "?language=ENGLISH&difficultyLevel=BEGINNER"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> rubricCriterionClient.getRubricCriteria(Language.ENGLISH, DifficultyLevel.BEGINNER));

        assertEquals("Admin Service returned no rubric criteria for ENGLISH BEGINNER", exception.getMessage());
        server.verify();
    }

    @Test
    void propagatesAdminServiceFailureWithoutFallback() {
        server.expect(requestTo("http://admin-service:8086/api/v1/admin/rubrics"
                        + "?language=ENGLISH&difficultyLevel=INTERMEDIATE"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(RestClientResponseException.class,
                () -> rubricCriterionClient.getRubricCriteria(Language.ENGLISH, DifficultyLevel.INTERMEDIATE));
        server.verify();
    }
}
