package com.nus.iss.service.impl;

import com.nus.iss.config.ModerationConfig.ModerationPolicy;
import com.nus.iss.dto.GradingRequestDTO;
import com.nus.iss.dto.AiGradingOutputDTO;
import com.nus.iss.model.CompositionModerationFlag;
import com.nus.iss.repository.CompositionModerationFlagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.moderation.CategoryScores;
import org.springframework.ai.moderation.Generation;
import org.springframework.ai.moderation.Moderation;
import org.springframework.ai.moderation.ModerationModel;
import org.springframework.ai.moderation.ModerationPrompt;
import org.springframework.ai.moderation.ModerationResponse;
import org.springframework.ai.moderation.ModerationResult;
import tools.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModerationServiceImplTest {

    @Test
    void blocksAndStoresOnlyDigestWhenCustomThresholdIsMet() {
        ModerationModel model = mock(ModerationModel.class);
        CompositionModerationFlagRepository repository = mock(CompositionModerationFlagRepository.class);
        when(model.call(any(ModerationPrompt.class))).thenReturn(response(false, 0.80));
        ModerationServiceImpl service = service(model, repository);

        assertThrows(com.nus.iss.exception.ModerationBlockedException.class,
                () -> service.moderateComposition(request()));

        var promptCaptor = org.mockito.ArgumentCaptor.forClass(ModerationPrompt.class);
        verify(model).call(promptCaptor.capture());
        assertEquals("Title: A day\n\nComposition:\nI went home.",
                promptCaptor.getValue().getInstructions().getText());

        var entityCaptor = org.mockito.ArgumentCaptor.forClass(CompositionModerationFlag.class);
        verify(repository).saveAndFlush(entityCaptor.capture());
        CompositionModerationFlag saved = entityCaptor.getValue();
        assertEquals("request-1", saved.getRequestId());
        assertEquals("workflow-1", saved.getContextId());
        assertEquals(10L, saved.getCompositionId());
        assertEquals(7L, saved.getUserId());
        assertEquals(64, saved.getContentSha256().length());
        assertEquals("COMPOSITION_INPUT", saved.getStage());
        assertEquals("BLOCKED", saved.getDecision());
        assertEquals("moderation-1", saved.getProviderResponseId());
        assertEquals("omni-moderation-latest", saved.getModel());
        assertEquals(0.80, saved.getThreshold());
        assertTrue(saved.getMatchedCategoryScores().contains("\"violence\":0.8"));
    }

    @Test
    void blocksWhenProviderFlagsBelowCustomThreshold() {
        ModerationModel model = mock(ModerationModel.class);
        CompositionModerationFlagRepository repository = mock(CompositionModerationFlagRepository.class);
        when(model.call(any(ModerationPrompt.class))).thenReturn(response(true, 0.7999));

        assertThrows(com.nus.iss.exception.ModerationBlockedException.class,
                () -> service(model, repository).moderateComposition(request()));

        verify(repository).saveAndFlush(any());
    }

    @Test
    void failsClosedWhenModerationApiFails() {
        ModerationModel model = mock(ModerationModel.class);
        CompositionModerationFlagRepository repository = mock(CompositionModerationFlagRepository.class);
        when(model.call(any(ModerationPrompt.class))).thenThrow(new IllegalStateException("unavailable"));

        assertThrows(com.nus.iss.exception.ModerationUnavailableException.class,
                () -> service(model, repository).moderateComposition(request()));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void failsClosedWhenModerationResponseHasNoResult() {
        ModerationModel model = mock(ModerationModel.class);
        CompositionModerationFlagRepository repository = mock(CompositionModerationFlagRepository.class);
        when(model.call(any(ModerationPrompt.class))).thenReturn(new ModerationResponse(null));

        assertThrows(com.nus.iss.exception.ModerationUnavailableException.class,
                () -> service(model, repository).moderateComposition(request()));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void failsClosedWhenFlagCannotBePersisted() {
        ModerationModel model = mock(ModerationModel.class);
        CompositionModerationFlagRepository repository = mock(CompositionModerationFlagRepository.class);
        when(model.call(any(ModerationPrompt.class))).thenReturn(response(true, 0.90));
        when(repository.saveAndFlush(any())).thenThrow(new IllegalStateException("database unavailable"));

        assertThrows(com.nus.iss.exception.ModerationUnavailableException.class,
                () -> service(model, repository).moderateComposition(request()));
    }

    @Test
    void blocksLearnerFeedbackBeforeItCanBeReturned() {
        ModerationModel model = mock(ModerationModel.class);
        CompositionModerationFlagRepository repository = mock(CompositionModerationFlagRepository.class);
        when(model.call(any(ModerationPrompt.class))).thenReturn(response(true, 0.90));

        assertThrows(com.nus.iss.exception.ModerationBlockedException.class,
                () -> service(model, repository).moderateFeedback(request(), AiGradingOutputDTO.builder().confidence(0.9).build()));

        var entityCaptor = org.mockito.ArgumentCaptor.forClass(CompositionModerationFlag.class);
        verify(repository).saveAndFlush(entityCaptor.capture());
        assertEquals("LEARNER_FEEDBACK", entityCaptor.getValue().getStage());
    }

    private static ModerationServiceImpl service(ModerationModel model,
                                                  CompositionModerationFlagRepository repository) {
        return new ModerationServiceImpl(model, new ModerationPolicy(0.80), repository, new ObjectMapper(), new SimpleMeterRegistry(), mock(ModerationAuditPublisher.class));
    }

    private static GradingRequestDTO request() {
        return GradingRequestDTO.builder()
                .requestId("request-1")
                .contextId("workflow-1")
                .compositionId(10L)
                .userId(7L)
                .title("A day")
                .content("I went home.")
                .build();
    }

    private static ModerationResponse response(boolean providerFlagged, double violenceScore) {
        CategoryScores scores = CategoryScores.builder().violence(violenceScore).build();
        ModerationResult result = ModerationResult.builder()
                .flagged(providerFlagged)
                .categoryScores(scores)
                .build();
        Moderation moderation = Moderation.builder()
                .id("moderation-1")
                .model("omni-moderation-latest")
                .results(List.of(result))
                .build();
        return new ModerationResponse(new Generation(moderation));
    }
}
