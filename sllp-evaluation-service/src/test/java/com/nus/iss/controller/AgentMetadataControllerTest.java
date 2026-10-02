package com.nus.iss.controller;

import com.nus.iss.config.SllpProperties;
import com.nus.iss.adapter.OpenAiStatusClient;
import com.nus.iss.repository.GradingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgentMetadataController.class)
class AgentMetadataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SllpProperties properties;

    @MockitoBean
    private OpenAiStatusClient openAiStatusClient;

    @MockitoBean
    private GradingRepository gradingRepository;

    @Test
    void returnsReadyWhenOpenAiIsOperational() throws Exception {
        when(openAiStatusClient.isOperational()).thenReturn(true);

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ready"))
                .andExpect(jsonPath("$.openai").value("operational"));
    }

    @Test
    void returnsServiceUnavailableWhenOpenAiIsNotOperational() throws Exception {
        when(openAiStatusClient.isOperational()).thenReturn(false);

        mockMvc.perform(get("/ready"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("unavailable"))
                .andExpect(jsonPath("$.openai").value("unavailable"));
    }

    @Test
    void livenessDoesNotCallOpenAi() throws Exception {
        mockMvc.perform(get("/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("alive"));

        verifyNoInteractions(openAiStatusClient);
    }
}
