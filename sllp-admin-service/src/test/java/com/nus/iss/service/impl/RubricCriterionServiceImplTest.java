package com.nus.iss.service.impl;

import com.nus.iss.dto.RubricCriterionRequest;
import com.nus.iss.dto.RubricCriterionResponse;
import com.nus.iss.entity.RubricCriterion;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import com.nus.iss.exception.RubricCriterionNotFoundException;
import com.nus.iss.repository.RubricCriterionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RubricCriterionServiceImplTest {

    @Mock
    private RubricCriterionRepository repository;

    @InjectMocks
    private RubricCriterionServiceImpl service;

    @Test
    void createTrimsTextAndStoresAuditActor() {
        when(repository.save(any(RubricCriterion.class))).thenAnswer(invocation -> {
            RubricCriterion criterion = invocation.getArgument(0);
            criterion.setId(7L);
            criterion.setCreatedAt(LocalDateTime.of(2026, 8, 4, 10, 0));
            criterion.setUpdatedAt(LocalDateTime.of(2026, 8, 4, 10, 0));
            return criterion;
        });

        RubricCriterionResponse response = service.create(request(), " alextan ");

        ArgumentCaptor<RubricCriterion> captor = ArgumentCaptor.forClass(RubricCriterion.class);
        verify(repository).save(captor.capture());
        RubricCriterion saved = captor.getValue();
        assertEquals("TASK_IDEAS", saved.getCriterionName());
        assertEquals("Assess relevant ideas.", saved.getCriterionDescription());
        assertEquals("alextan", saved.getCreatedBy());
        assertEquals("alextan", saved.getUpdatedBy());
        assertEquals(7L, response.id());
        assertEquals(new BigDecimal("25.00"), response.weightPercentage());
    }

    @Test
    void getAllWithBothFiltersUsesFilteredRepositoryQuery() {
        RubricCriterion criterion = persistedCriterion();
        when(repository.findByLanguageAndDifficultyLevel(
                org.mockito.ArgumentMatchers.eq(Language.ENGLISH),
                org.mockito.ArgumentMatchers.eq(DifficultyLevel.BEGINNER),
                any(Sort.class))).thenReturn(List.of(criterion));

        List<RubricCriterionResponse> response = service.getAll(Language.ENGLISH, DifficultyLevel.BEGINNER);

        assertEquals(1, response.size());
        assertEquals(7L, response.getFirst().id());
    }

    @Test
    void getAllSupportsEachSingleFilterAndNoFilters() {
        RubricCriterion criterion = persistedCriterion();
        when(repository.findByLanguage(org.mockito.ArgumentMatchers.eq(Language.ENGLISH), any(Sort.class)))
                .thenReturn(List.of(criterion));
        when(repository.findByDifficultyLevel(org.mockito.ArgumentMatchers.eq(DifficultyLevel.BEGINNER), any(Sort.class)))
                .thenReturn(List.of(criterion));
        when(repository.findAll(any(Sort.class))).thenReturn(List.of(criterion));

        assertEquals(1, service.getAll(Language.ENGLISH, null).size());
        assertEquals(1, service.getAll(null, DifficultyLevel.BEGINNER).size());
        assertEquals(1, service.getAll(null, null).size());
    }

    @Test
    void getByIdWhenMissingThrowsNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        RubricCriterionNotFoundException exception = assertThrows(
                RubricCriterionNotFoundException.class, () -> service.getById(99L));

        assertEquals("Rubric criterion with ID 99 was not found", exception.getMessage());
    }

    @Test
    void updateReplacesEditableFieldsAndPreservesCreator() {
        RubricCriterion criterion = persistedCriterion();
        when(repository.findById(7L)).thenReturn(Optional.of(criterion));
        when(repository.save(criterion)).thenReturn(criterion);
        RubricCriterionRequest update = new RubricCriterionRequest(
                Language.CHINESE,
                DifficultyLevel.ADVANCED,
                " CONTENT_RELEVANCE ",
                " Advanced content description. ",
                new BigDecimal("30.00"));

        RubricCriterionResponse response = service.update(7L, update, "editor");

        assertEquals(Language.CHINESE, criterion.getLanguage());
        assertEquals(DifficultyLevel.ADVANCED, criterion.getDifficultyLevel());
        assertEquals("CONTENT_RELEVANCE", criterion.getCriterionName());
        assertEquals("Advanced content description.", criterion.getCriterionDescription());
        assertEquals(new BigDecimal("30.00"), criterion.getWeightPercentage());
        assertEquals("creator", criterion.getCreatedBy());
        assertEquals("editor", response.updatedBy());
    }

    @Test
    void deleteWhenPresentDeletesLoadedCriterion() {
        RubricCriterion criterion = persistedCriterion();
        when(repository.findById(7L)).thenReturn(Optional.of(criterion));

        service.delete(7L);

        verify(repository).delete(criterion);
    }

    @Test
    void invalidIdIsRejectedBeforeRepositoryLookup() {
        assertThrows(IllegalArgumentException.class, () -> service.getById(0L));
        assertThrows(IllegalArgumentException.class, () -> service.getById(null));

        verify(repository, never()).findById(any());
    }

    @Test
    void missingUsernameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.create(request(), null));

        verify(repository, never()).save(any());
    }

    @Test
    void blankUsernameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.create(request(), " "));

        verify(repository, never()).save(any());
    }

    @Test
    void usernameLongerThanColumnLimitIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.create(request(), "a".repeat(101)));

        verify(repository, never()).save(any());
    }

    private static RubricCriterionRequest request() {
        return new RubricCriterionRequest(
                Language.ENGLISH,
                DifficultyLevel.BEGINNER,
                " TASK_IDEAS ",
                " Assess relevant ideas. ",
                new BigDecimal("25.00"));
    }

    private static RubricCriterion persistedCriterion() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 4, 9, 0);
        return RubricCriterion.builder()
                .id(7L)
                .language(Language.ENGLISH)
                .difficultyLevel(DifficultyLevel.BEGINNER)
                .criterionName("TASK_IDEAS")
                .criterionDescription("Assess relevant ideas.")
                .weightPercentage(new BigDecimal("25.00"))
                .createdBy("creator")
                .updatedBy("creator")
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }
}
