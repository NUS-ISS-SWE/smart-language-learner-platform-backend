package com.nus.iss.adapter;

import com.nus.iss.config.SllpProperties;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
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

class SllpAdminClientTest {
    private MockRestServiceServer server;
    private SllpAdminClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        SllpProperties properties = new SllpProperties();
        properties.getServices().setAdminServiceUrl("https://admin.test");
        client = new SllpAdminClient(builder, properties);
    }

    @Test
    void returnsMatchingAdminRubric() {
        server.expect(once(), requestTo("https://admin.test/api/v1/admin/rubrics"
                        + "?language=ENGLISH&difficultyLevel=BEGINNER"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"id":1,"language":"ENGLISH","difficultyLevel":"BEGINNER",
                          "criterionName":"TASK_IDEAS","criterionDescription":"Develop ideas.",
                          "weightPercentage":100,"createdBy":"SYSTEM","updatedBy":"SYSTEM"}]
                        """, MediaType.APPLICATION_JSON));

        var result = client.getRubricCriteria(Language.ENGLISH, DifficultyLevel.BEGINNER);

        assertEquals(1, result.size());
        assertEquals("TASK_IDEAS", result.getFirst().criterionName());
        server.verify();
    }

    @Test
    void rejectsEmptyAdminRubric() {
        server.expect(once(), requestTo("https://admin.test/api/v1/admin/rubrics"
                        + "?language=CHINESE&difficultyLevel=ADVANCED"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThrows(IllegalStateException.class,
                () -> client.getRubricCriteria(Language.CHINESE, DifficultyLevel.ADVANCED));
    }

    @Test
    void wrapsAdminServiceFailure() {
        server.expect(once(), requestTo("https://admin.test/api/v1/admin/rubrics"
                        + "?language=ENGLISH&difficultyLevel=INTERMEDIATE"))
                .andRespond(withServerError());

        assertThrows(IllegalStateException.class,
                () -> client.getRubricCriteria(Language.ENGLISH, DifficultyLevel.INTERMEDIATE));
    }
}
