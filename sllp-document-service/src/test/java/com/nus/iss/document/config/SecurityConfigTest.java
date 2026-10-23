package com.nus.iss.document.config;

import com.nus.iss.document.controller.DocumentController;
import com.nus.iss.document.security.JwtAuthenticationFilter;
import com.nus.iss.document.security.JwtProvider;
import com.nus.iss.document.service.DocumentService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The document service's own security chain (behind the gateway): only requests carrying a valid
 * JWT reach a controller. Web slice only — no database, S3 or ClamAV is started.
 */
@WebMvcTest(controllers = DocumentController.class,
        // Publishes audit events to admin-service over HTTP; not part of the security chain.
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AuditHttpFilter.class))
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtProvider.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    // @EnableJpaAuditing on the application class needs this even in a web slice.
    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Value("${application.jwt.secret}")
    private String jwtSecret;

    @Test
    void requestWithoutTokenNeverReachesTheController() throws Exception {
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isForbidden());

        verify(documentService, never()).listUserDocuments(anyString(), anyInt(), anyInt());
    }

    @Test
    void requestWithForgedTokenNeverReachesTheController() throws Exception {
        String forged = Jwts.builder()
                .claim("userId", 42)
                .claim("ROLE", "ADMIN")
                .signWith(Keys.hmacShaKeyFor("another-secret-that-is-long-enough-for-hmac-sha".getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/api/documents").header("Authorization", "Bearer " + forged))
                .andExpect(status().isForbidden());

        verify(documentService, never()).listUserDocuments(anyString(), anyInt(), anyInt());
    }

    @Test
    void validTokenReachesTheControllerAsThatUser() throws Exception {
        String token = Jwts.builder()
                .claim("userId", 42)
                .claim("email", "alex@example.com")
                .claim("ROLE", "USER_BASIC")
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/api/documents").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        verify(documentService).listUserDocuments("42", 0, 20);
    }

    @Test
    void healthEndpointIsOpen() throws Exception {
        // No actuator in a web slice, so the request fails further down — the point is only
        // that security lets it through without a token.
        mockMvc.perform(get("/actuator/health"))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus()));
    }
}
