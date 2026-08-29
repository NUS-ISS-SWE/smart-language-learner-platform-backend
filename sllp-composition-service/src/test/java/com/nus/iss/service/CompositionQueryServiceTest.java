package com.nus.iss.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.nus.iss.dto.CompositionResponse;
import com.nus.iss.entity.Composition;
import com.nus.iss.enums.CompositionStatus;
import com.nus.iss.repository.CompositionRepository;

@ExtendWith(MockitoExtension.class)
class CompositionQueryServiceTest {
    @Mock private CompositionRepository compositionRepository;

    private CompositionQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new CompositionQueryService(compositionRepository,
                new CompositionAccessService(compositionRepository), new CompositionMapper());
    }

    @Test
    void listValidatesInputsAndMapsRepositoryResults() {
        PageRequest page = PageRequest.of(0, 20);
        when(compositionRepository.findByUserId(1L, page)).thenReturn(List.of(composition()));

        CompositionResponse response = queryService.listForUser(1L, page).getFirst();

        assertEquals(10L, response.id());
        assertEquals(1L, response.userId());
        assertEquals("Title", response.title());
    }

    @Test
    void listRejectsInvalidUserAndPagination() {
        assertThrows(IllegalArgumentException.class, () -> queryService.listForUser(0L, PageRequest.of(0, 20)));
        assertThrows(IllegalArgumentException.class, () -> queryService.listForUser(1L, null));
        assertThrows(IllegalArgumentException.class, () -> queryService.listForUser(1L, PageRequest.of(0, 101)));
    }

    @Test
    void userAndAdminReadsMapExistingCompositions() {
        Composition composition = composition();
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(composition));
        when(compositionRepository.findById(10L)).thenReturn(Optional.of(composition));

        assertEquals(10L, queryService.getForUser(1L, 10L).id());
        assertEquals(10L, queryService.getForAdmin(10L).id());
    }

    @Test
    void userAndAdminReadsRejectInvalidAndMissingCompositions() {
        assertThrows(IllegalArgumentException.class, () -> queryService.getForUser(1L, 0L));
        assertThrows(IllegalArgumentException.class, () -> queryService.getForAdmin(null));

        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());
        when(compositionRepository.findById(10L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> queryService.getForUser(1L, 10L));
        assertThrows(NoSuchElementException.class, () -> queryService.getForAdmin(10L));
    }

    @Test
    void accessServiceRejectsNegativePageNumbers() {
        Pageable invalidPage = org.mockito.Mockito.mock(Pageable.class);
        when(invalidPage.getPageNumber()).thenReturn(-1);

        assertThrows(IllegalArgumentException.class, () -> queryService.listForUser(1L, invalidPage));
    }

    private static Composition composition() {
        return Composition.builder().id(10L).userId(1L).title("Title").content("Text")
                .compositionStatus(CompositionStatus.DRAFT).build();
    }
}
