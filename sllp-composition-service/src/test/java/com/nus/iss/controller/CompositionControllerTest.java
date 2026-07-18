package com.nus.iss.controller;

import com.nus.iss.dto.CompositionDTO;
import com.nus.iss.enums.CompositionStatus;
import com.nus.iss.service.CompositionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompositionControllerTest {

    @Mock
    private CompositionService compositionService;

    @Test
    void createNewCompositionReturnsCreatedComposition() {
        CompositionController controller = new CompositionController(compositionService);
        CompositionDTO request = request();
        CompositionDTO createdComposition = response();
        when(compositionService.createNewComposition("ming", request)).thenReturn(createdComposition);

        ResponseEntity<CompositionDTO> response = controller.createNewComposition("ming", "USER_FREE_TRIAL", request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(createdComposition, response.getBody());
    }

    @Test
    void getAllCompositionByUsernameUsesDescendingCreatedAtPageRequest() {
        CompositionController controller = new CompositionController(compositionService);
        List<CompositionDTO> compositions = List.of(response());
        when(compositionService.getAllCompositionByUsername(org.mockito.ArgumentMatchers.eq("ming"),
                org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(compositions);

        ResponseEntity<List<CompositionDTO>> response =
                controller.getAllCompositionByUsername("ming", "USER_FREE_TRIAL", 2, 20);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(compositionService).getAllCompositionByUsername(org.mockito.ArgumentMatchers.eq("ming"), pageableCaptor.capture());
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
        when(compositionService.getSingleCompositionById("ming", 10L)).thenReturn(composition);

        ResponseEntity<CompositionDTO> response =
                controller.getSingleCompositionById("ming", "USER_FREE_TRIAL", 10L);

        assertEquals(200, response.getStatusCode().value());
        assertSame(composition, response.getBody());
    }

    @Test
    void updateCompositionReturnsUpdatedComposition() {
        CompositionController controller = new CompositionController(compositionService);
        CompositionDTO request = request();
        CompositionDTO updatedComposition = response();
        when(compositionService.updateComposition("ming", 10L, request)).thenReturn(updatedComposition);

        ResponseEntity<CompositionDTO> response =
                controller.updateComposition("ming", "USER_FREE_TRIAL", 10L, request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(updatedComposition, response.getBody());
    }

    @Test
    void deleteCompositionReturnsNoContent() {
        CompositionController controller = new CompositionController(compositionService);

        ResponseEntity<CompositionDTO> response =
                controller.deleteComposition("ming", "USER_FREE_TRIAL", 10L);

        assertEquals(204, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(compositionService).deleteComposition("ming", 10L);
    }

    @Test
    void requestToGradeCompositionReturnsReviewingComposition() {
        CompositionController controller = new CompositionController(compositionService);
        CompositionDTO reviewingComposition = response();
        reviewingComposition.setCompositionStatus(CompositionStatus.REVIEWING);
        when(compositionService.requestToGradeComposition("ming", 10L)).thenReturn(reviewingComposition);

        ResponseEntity<CompositionDTO> response =
                controller.requestToGradeComposition("ming", "USER_FREE_TRIAL", 10L);

        assertEquals(200, response.getStatusCode().value());
        assertSame(reviewingComposition, response.getBody());
    }

    @Test
    void generateCompositionReturnsGeneratingComposition() {
        CompositionController controller = new CompositionController(compositionService);
        CompositionDTO request = CompositionDTO.builder().title("Generate this").build();
        CompositionDTO generatedComposition = response();
        generatedComposition.setCompositionStatus(CompositionStatus.GENERATING);
        when(compositionService.generateComposition("ming", request)).thenReturn(generatedComposition);

        ResponseEntity<CompositionDTO> response =
                controller.generateComposition("ming", "USER_FREE_TRIAL", request);

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
                .username("ming")
                .title("My title")
                .content("This is my composition.")
                .compositionStatus(CompositionStatus.DRAFT)
                .build();
    }
}
