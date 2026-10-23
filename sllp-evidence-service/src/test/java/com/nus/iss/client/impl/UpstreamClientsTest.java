package com.nus.iss.client.impl;

import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.CompositionDTO;
import com.nus.iss.dto.RubricCriterionDTO;
import com.nus.iss.dto.UserProfileDTO;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import com.nus.iss.exception.UpstreamServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * The three services evidence collection depends on: each call must hit the right endpoint, and any
 * failure or empty answer must surface as {@link UpstreamServiceException} (502), never as a null.
 */
class UpstreamClientsTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private SllpProperties properties;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        properties = new SllpProperties();
        properties.getServices().setCompositionServiceUrl("http://composition-service:8083");
        properties.getServices().setAdminServiceUrl("http://admin-service:8086");
        properties.getServices().setUserServiceUrl("http://user-service:8082");
    }

    // --- composition-service ---

    @Test
    void compositionIsFetchedOnBehalfOfItsOwner() {
        server.expect(requestTo("http://composition-service:8083/api/v1/composition/12"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Sllp-User-Id", "7"))
                .andRespond(withSuccess("""
                        {"id":12,"userId":7,"title":"My holiday","content":"I went to Penang.","language":"ENGLISH"}
                        """, MediaType.APPLICATION_JSON));

        CompositionDTO composition = new CompositionClientImpl(builder, properties).getComposition(7L, 12L);

        assertEquals("I went to Penang.", composition.content());
        assertEquals(Language.ENGLISH, composition.language());
        server.verify();
    }

    @Test
    void compositionServiceErrorIsAnUpstreamFailure() {
        server.expect(requestTo("http://composition-service:8083/api/v1/composition/12")).andRespond(withServerError());

        UpstreamServiceException error = assertThrows(UpstreamServiceException.class,
                () -> new CompositionClientImpl(builder, properties).getComposition(7L, 12L));
        assertTrue(error.getMessage().contains("composition-service"));
    }

    @Test
    void emptyCompositionResponseIsAnUpstreamFailure() {
        server.expect(requestTo("http://composition-service:8083/api/v1/composition/12"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThrows(UpstreamServiceException.class,
                () -> new CompositionClientImpl(builder, properties).getComposition(7L, 12L));
    }

    // --- admin-service (rubric) ---

    @Test
    void rubricIsFetchedForTheLanguageAndDifficulty() {
        server.expect(requestTo("http://admin-service:8086/api/v1/admin/rubrics?language=ENGLISH&difficultyLevel=INTERMEDIATE"))
                .andRespond(withSuccess("""
                        [{"id":1,"language":"ENGLISH","difficultyLevel":"INTERMEDIATE","criterionName":"Organization",
                          "criterionDescription":"Logical order","weightPercentage":40.00}]
                        """, MediaType.APPLICATION_JSON));

        List<RubricCriterionDTO> rubric = new RubricCriterionClientImpl(builder, properties)
                .getRubricCriteria(Language.ENGLISH, DifficultyLevel.INTERMEDIATE);

        assertEquals(1, rubric.size());
        assertEquals("Organization", rubric.getFirst().criterionName());
        server.verify();
    }

    @Test
    void missingRubricIsAnUpstreamFailure() {
        server.expect(requestTo("http://admin-service:8086/api/v1/admin/rubrics?language=CHINESE&difficultyLevel=ADVANCED"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        UpstreamServiceException error = assertThrows(UpstreamServiceException.class,
                () -> new RubricCriterionClientImpl(builder, properties).getRubricCriteria(Language.CHINESE, DifficultyLevel.ADVANCED));
        assertTrue(error.getMessage().contains("no rubric for CHINESE ADVANCED"));
    }

    @Test
    void adminServiceErrorIsAnUpstreamFailure() {
        server.expect(requestTo("http://admin-service:8086/api/v1/admin/rubrics?language=ENGLISH&difficultyLevel=BEGINNER"))
                .andRespond(withServerError());

        assertThrows(UpstreamServiceException.class,
                () -> new RubricCriterionClientImpl(builder, properties).getRubricCriteria(Language.ENGLISH, DifficultyLevel.BEGINNER));
    }

    // --- user-service ---

    @Test
    void difficultyLevelComesFromTheUserProfile() {
        server.expect(requestTo("http://user-service:8082/api/v1/auth/profile/7"))
                .andRespond(withSuccess("""
                        {"id":7,"username":"ming","difficultyLevel":"ADVANCED"}
                        """, MediaType.APPLICATION_JSON));

        UserProfileDTO profile = new UserProfileClientImpl(builder, properties).getUserProfile(7L);

        assertEquals(DifficultyLevel.ADVANCED, profile.difficultyLevel());
        server.verify();
    }

    @Test
    void profileWithoutDifficultyIsAnUpstreamFailure() {
        server.expect(requestTo("http://user-service:8082/api/v1/auth/profile/7"))
                .andRespond(withSuccess("""
                        {"id":7,"username":"ming"}
                        """, MediaType.APPLICATION_JSON));

        assertThrows(UpstreamServiceException.class,
                () -> new UserProfileClientImpl(builder, properties).getUserProfile(7L));
    }

    @Test
    void userServiceErrorIsAnUpstreamFailure() {
        server.expect(requestTo("http://user-service:8082/api/v1/auth/profile/7")).andRespond(withServerError());

        assertThrows(UpstreamServiceException.class,
                () -> new UserProfileClientImpl(builder, properties).getUserProfile(7L));
    }
}
