package com.nus.iss.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nus.iss.dto.CompositionGenerationRequestedEvent;
import com.nus.iss.contracts.grading.CompositionGradingRequestedEvent;
import com.nus.iss.dto.CompositionResponse;
import com.nus.iss.dto.CreateCompositionRequest;
import com.nus.iss.dto.GenerateCompositionRequest;
import com.nus.iss.dto.UpdateCompositionRequest;
import com.nus.iss.entity.Composition;
import com.nus.iss.enums.CompositionSource;
import com.nus.iss.enums.CompositionStatus;
import com.nus.iss.enums.Language;
import com.nus.iss.repository.CompositionRepository;

@ExtendWith(MockitoExtension.class)
class CompositionCommandServiceTest {
    @Mock private CompositionRepository compositionRepository;
    @Mock private LanguageDetectorService languageDetectorService;
    @Mock private KafkaProducerService kafkaProducerService;

    private CompositionCommandService commandService;

    @BeforeEach
    void setUp() {
        commandService = new CompositionCommandService(
                compositionRepository, new CompositionAccessService(compositionRepository),
                new CompositionMapper(), languageDetectorService, kafkaProducerService);
    }

    @Test
    void createPersistsDetectedLanguageRawTextDraft() {
        when(languageDetectorService.detectLanguage("English text")).thenReturn(Optional.of(Language.ENGLISH));
        when(compositionRepository.save(any(Composition.class))).thenAnswer(invocation -> {
            Composition composition = invocation.getArgument(0);
            composition.setId(10L);
            return composition;
        });

        CompositionResponse response = commandService.create(1L, new CreateCompositionRequest("Title", "English text"));

        assertEquals(10L, response.id());
        assertEquals(1L, response.userId());
        assertEquals(CompositionSource.RAW_TEXT, response.compositionSource());
        assertEquals(CompositionStatus.DRAFT, response.compositionStatus());
        assertEquals(Language.ENGLISH, response.language());
        assertNotNull(response.createdAt());
        verifyNoInteractions(kafkaProducerService);
    }

    @Test
    void createRejectsInvalidUserRequestAndUnsupportedLanguage() {
        assertThrows(IllegalArgumentException.class, () -> commandService.create(0L, createRequest()));
        assertThrows(IllegalArgumentException.class, () -> commandService.create(1L, null));
        assertThrows(IllegalArgumentException.class, () -> commandService.create(1L, new CreateCompositionRequest(" ", "Text")));
        assertThrows(IllegalArgumentException.class, () -> commandService.create(1L, new CreateCompositionRequest("Title", " ")));

        when(languageDetectorService.detectLanguage("English text")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> commandService.create(1L, createRequest()));
        verify(compositionRepository, never()).save(any());
        verifyNoInteractions(kafkaProducerService);
    }

    @Test
    void updateChangesDraftLanguageAndTimestamp() {
        Composition composition = composition();
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(composition));
        when(languageDetectorService.detectLanguage("Chinese text")).thenReturn(Optional.of(Language.CHINESE));
        when(compositionRepository.save(composition)).thenReturn(composition);

        CompositionResponse response = commandService.update(1L, 10L, new UpdateCompositionRequest("Updated", "Chinese text"));

        assertEquals("Updated", response.title());
        assertEquals("Chinese text", response.content());
        assertEquals(Language.CHINESE, response.language());
        assertEquals(CompositionStatus.DRAFT, response.compositionStatus());
        verify(compositionRepository).save(composition);
    }

    @Test
    void updateRejectsInvalidRequestMissingCompositionAndUnsupportedLanguage() {
        assertThrows(IllegalArgumentException.class,
                () -> commandService.update(1L, 10L, new UpdateCompositionRequest("Title", " ")));
        assertThrows(IllegalArgumentException.class,
                () -> commandService.update(1L, null, updateRequest()));

        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> commandService.update(1L, 10L, updateRequest()));

        Composition composition = composition();
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(composition));
        when(languageDetectorService.detectLanguage("Chinese text")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> commandService.update(1L, 10L, updateRequest()));
        verify(compositionRepository, never()).save(composition);
    }

    @Test
    void deleteDeletesOwnedCompositionAndDoesNotDeleteMissingComposition() {
        Composition composition = composition();
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(composition));

        commandService.delete(1L, 10L);
        verify(compositionRepository).delete(composition);

        when(compositionRepository.findByIdAndUserId(11L, 1L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> commandService.delete(1L, 11L));
    }

    @Test
    void requestGradingTransitionsDraftAndPublishesCompleteEvent() {
        Composition composition = composition();
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(composition));
        when(compositionRepository.save(composition)).thenReturn(composition);

        CompositionResponse response = commandService.requestGrading(1L, 10L);

        ArgumentCaptor<CompositionGradingRequestedEvent> captor =
                ArgumentCaptor.forClass(CompositionGradingRequestedEvent.class);
        verify(kafkaProducerService).sendCompositionGradingRequestedEvent(captor.capture());
        assertEquals(CompositionStatus.REVIEWING, response.compositionStatus());
        assertEquals(10L, captor.getValue().getCompositionId());
        assertEquals(1L, captor.getValue().getUserId());
        assertEquals("My title", captor.getValue().getTitle());
        assertEquals("English text", captor.getValue().getContent());
        assertEquals(Language.ENGLISH.name(), captor.getValue().getLanguage());
        assertNotNull(captor.getValue().getRequestedAt());
    }

    @Test
    void requestGradingRejectsNonDraftAndMissingComposition() {
        Composition reviewed = composition();
        reviewed.setCompositionStatus(CompositionStatus.REVIEWED);
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(reviewed));
        assertThrows(IllegalArgumentException.class, () -> commandService.requestGrading(1L, 10L));
        verify(compositionRepository, never()).save(any());
        verifyNoInteractions(kafkaProducerService);

        when(compositionRepository.findByIdAndUserId(11L, 1L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> commandService.requestGrading(1L, 11L));
    }

    @Test
    void generatePersistsSystemCompositionAndPublishesEvent() {
        when(languageDetectorService.detectLanguage("Chinese prompt")).thenReturn(Optional.of(Language.CHINESE));
        when(compositionRepository.save(any(Composition.class))).thenAnswer(invocation -> {
            Composition composition = invocation.getArgument(0);
            composition.setId(20L);
            return composition;
        });

        CompositionResponse response = commandService.generate(1L, new GenerateCompositionRequest("Chinese prompt"));

        ArgumentCaptor<CompositionGenerationRequestedEvent> captor =
                ArgumentCaptor.forClass(CompositionGenerationRequestedEvent.class);
        verify(kafkaProducerService).sendCompositionGenerationRequestedEvent(captor.capture());
        assertEquals(CompositionSource.SYSTEM, response.compositionSource());
        assertEquals(CompositionStatus.GENERATING, response.compositionStatus());
        assertEquals(Language.CHINESE, response.language());
        assertEquals(20L, captor.getValue().getCompositionId());
        assertEquals(Language.CHINESE, captor.getValue().getLanguage());
    }

    @Test
    void generateDefaultsLanguageAndRejectsInvalidRequest() {
        when(languageDetectorService.detectLanguage("12345")).thenReturn(Optional.empty());
        when(compositionRepository.save(any(Composition.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(Language.ENGLISH, commandService.generate(1L, new GenerateCompositionRequest("12345")).language());
        assertThrows(IllegalArgumentException.class, () -> commandService.generate(1L, null));
        assertThrows(IllegalArgumentException.class, () -> commandService.generate(1L, new GenerateCompositionRequest(" ")));
    }

    private static CreateCompositionRequest createRequest() {
        return new CreateCompositionRequest("My title", "English text");
    }

    private static UpdateCompositionRequest updateRequest() {
        return new UpdateCompositionRequest("Updated", "Chinese text");
    }

    private static Composition composition() {
        return Composition.builder().id(10L).userId(1L).title("My title").content("English text")
                .compositionSource(CompositionSource.RAW_TEXT).language(Language.ENGLISH)
                .compositionStatus(CompositionStatus.DRAFT).build();
    }
}
