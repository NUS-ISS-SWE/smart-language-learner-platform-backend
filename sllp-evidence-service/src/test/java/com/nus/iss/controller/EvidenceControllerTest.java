package com.nus.iss.controller;

import com.nus.iss.dto.EvidenceCollectionResult;
import com.nus.iss.service.EvidenceCollectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for the trigger adapter only — no LLM, no real outbound calls. Confirms the
 * controller wires request/response correctly; {@link EvidenceCollectionService} is mocked.
 */
@WebMvcTest(EvidenceController.class)
class EvidenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EvidenceCollectionService evidenceCollectionService;

    @Test
    void collectDelegatesToServiceAndReturnsResult() throws Exception {
        EvidenceCollectionResult result = new EvidenceCollectionResult(42L, List.of("plan"), List.of(), null);
        when(evidenceCollectionService.collect(7L, 42L)).thenReturn(result);

        mockMvc.perform(post("/api/v1/evidence/42/collect").header("X-Sllp-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compositionId").value(42));

        verify(evidenceCollectionService).collect(eq(7L), eq(42L));
    }

    @Test
    void getLastReturns404WhenNothingStoredYet() throws Exception {
        when(evidenceCollectionService.getLast(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/evidence/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getLastReturnsStoredResult() throws Exception {
        EvidenceCollectionResult result = new EvidenceCollectionResult(42L, List.of("plan"), List.of(), null);
        when(evidenceCollectionService.getLast(42L)).thenReturn(Optional.of(result));

        mockMvc.perform(get("/api/v1/evidence/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compositionId").value(42));
    }
}
