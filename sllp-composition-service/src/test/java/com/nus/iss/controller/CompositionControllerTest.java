package com.nus.iss.controller;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nus.iss.dto.CompositionDTO;
import com.nus.iss.enums.CompositionStatus;
import com.nus.iss.service.CompositionService;

@ExtendWith(MockitoExtension.class)
class CompositionControllerTest {

    @Mock
    private CompositionService compositionService;

    @Test
    void createNewCompositionReturnsCreatedComposition() {
        CompositionController controller = new CompositionController(compositionService);
        CompositionDTO request = request();
        CompositionDTO createdComposition = response();
        when(compositionService.createNewComposition(1L, request)).thenReturn(createdComposition);

        ResponseEntity<CompositionDTO> response = controller.createNewComposition(1L, "USER_FREE_TRIAL", request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(createdComposition, response.getBody());
    }

    @Test
    void getAllCompositionByUserIdUsesDescendingCreatedAtPageRequest() {
        CompositionController controller = new CompositionController(compositionService);
        List<CompositionDTO> compositions = List.of(response());
        when(compositionService.getAllCompositionByUserId(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(compositions);

        ResponseEntity<List<CompositionDTO>> response =
                controller.getAllCompositionByUserId(1L, "USER_FREE_TRIAL", 2, 20);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(compositionService).getAllCompositionByUserId(org.mockito.ArgumentMatchers.eq(1L), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();

        assertEquals(200, response.getStatusCode().value());
        assertSame(compositions, response.getBody());
        assertEquals(2, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        assertEquals("createdAt: DESC", pageable.getSort().toString());
    }

    @Test
    void getSingleCompositionByIdReturnsComposition() {
        CompositionController controller = new CompositionController(compositionService);
        CompositionDTO composition = response();
        when(compositionService.getSingleCompositionById(1L, 10L)).thenReturn(composition);

        ResponseEntity<CompositionDTO> response =
                controller.getSingleCompositionById(1L, "USER_FREE_TRIAL", 10L);

        assertEquals(200, response.getStatusCode().value());
        assertSame(composition, response.getBody());
    }

    @Test
    void updateCompositionReturnsUpdatedComposition() {
        CompositionController controller = new CompositionController(compositionService);
        CompositionDTO request = request();
        CompositionDTO updatedComposition = response();
        when(compositionService.updateComposition(1L, 10L, request)).thenReturn(updatedComposition);

        ResponseEntity<CompositionDTO> response =
                controller.updateComposition(1L, "USER_FREE_TRIAL", 10L, request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(updatedComposition, response.getBody());
    }

    @Test
    void deleteCompositionReturnsNoContent() {
        CompositionController controller = new CompositionController(compositionService);

        ResponseEntity<Void> response =
                controller.deleteComposition(1L, "USER_FREE_TRIAL", 10L);

        assertEquals(204, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(compositionService).deleteComposition(1L, 10L);
    }

    @Test
    void requestToGradeCompositionReturnsReviewingComposition() {
        CompositionController controller = new CompositionController(compositionService);
        CompositionDTO reviewingComposition = response();
        reviewingComposition.setCompositionStatus(CompositionStatus.REVIEWING);
        when(compositionService.requestToGradeComposition(1L, 10L)).thenReturn(reviewingComposition);

        ResponseEntity<CompositionDTO> response =
                controller.requestToGradeComposition(1L, "USER_FREE_TRIAL", 10L);

        assertEquals(200, response.getStatusCode().value());
        assertSame(reviewingComposition, response.getBody());
    }

    @Test
    void generateCompositionReturnsGeneratingComposition() {
        CompositionController controller = new CompositionController(compositionService);
        CompositionDTO request = CompositionDTO.builder().title("Generate this").build();
        CompositionDTO generatedComposition = response();
        generatedComposition.setCompositionStatus(CompositionStatus.GENERATING);
        when(compositionService.generateComposition(1L, request)).thenReturn(generatedComposition);

        ResponseEntity<CompositionDTO> response =
                controller.generateComposition(1L, "USER_FREE_TRIAL", request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(generatedComposition, response.getBody());
    }

    private static CompositionDTO request() {
        return CompositionDTO.builder()
                .title("My title")
                .content("This is my composition.")
                .build();
    }

    private static CompositionDTO response() {
        return CompositionDTO.builder()
                .id(10L)
                .userId(1L)
                .title("My title")
                .content("This is my composition.")
                .compositionStatus(CompositionStatus.DRAFT)
                .build();
    }
}
