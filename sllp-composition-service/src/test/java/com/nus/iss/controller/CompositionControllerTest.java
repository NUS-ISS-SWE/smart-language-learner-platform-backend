package com.nus.iss.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import com.nus.iss.dto.CompositionExampleResponse;
import com.nus.iss.dto.CompositionResponse;
import com.nus.iss.dto.CreateCompositionRequest;
import com.nus.iss.dto.GenerateCompositionRequest;
import com.nus.iss.dto.UpdateCompositionRequest;
import com.nus.iss.enums.CompositionStatus;
import com.nus.iss.enums.Language;
import com.nus.iss.service.CompositionCommandService;
import com.nus.iss.service.CompositionExampleService;
import com.nus.iss.service.CompositionQueryService;

@ExtendWith(MockitoExtension.class)
class CompositionControllerTest {
    @Mock private CompositionCommandService commandService;
    @Mock private CompositionQueryService queryService;
    @Mock private CompositionExampleService exampleService;

    @Test
    void createDelegatesTypedRequestToCommandService() {
        CompositionController controller = controller();
        CreateCompositionRequest request = new CreateCompositionRequest("My title", "My composition");
        CompositionResponse created = response();
        when(commandService.create(1L, request)).thenReturn(created);

        ResponseEntity<CompositionResponse> result = controller.createNewComposition(1L, request);

        assertEquals(200, result.getStatusCode().value());
        assertSame(created, result.getBody());
    }

    @Test
    void listUsesDescendingCreatedAtPageRequest() {
        CompositionController controller = controller();
        List<CompositionResponse> compositions = List.of(response());
        when(queryService.listForUser(eq(1L), any(Pageable.class))).thenReturn(compositions);

        ResponseEntity<List<CompositionResponse>> result = controller.getAllCompositionByUserId(1L, 2, 20);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(queryService).listForUser(eq(1L), captor.capture());
        assertEquals(2, captor.getValue().getPageNumber());
        assertEquals(20, captor.getValue().getPageSize());
        assertEquals("createdAt: DESC", captor.getValue().getSort().toString());
        assertSame(compositions, result.getBody());
    }

    @Test
    void updateAndGenerateDelegatePurposeSpecificRequests() {
        CompositionController controller = controller();
        CompositionResponse updated = response();
        when(commandService.update(1L, 10L, new UpdateCompositionRequest("New", "Updated"))).thenReturn(updated);
        when(commandService.generate(1L, new GenerateCompositionRequest("Generate"))).thenReturn(updated);

        assertSame(updated, controller.updateComposition(1L, 10L, new UpdateCompositionRequest("New", "Updated")).getBody());
        assertSame(updated, controller.generateComposition(1L, new GenerateCompositionRequest("Generate")).getBody());
    }

    @Test
    void deleteAndGradingDelegateToCommandService() {
        CompositionController controller = controller();
        when(commandService.requestGrading(1L, 10L)).thenReturn(response());

        assertEquals(204, controller.deleteComposition(1L, 10L).getStatusCode().value());
        assertEquals(200, controller.requestToGradeComposition(1L, 10L).getStatusCode().value());
        verify(commandService).delete(1L, 10L);
        verify(commandService).requestGrading(1L, 10L);
    }

    @Test
    void getAllExamplesReturnsExamplesFromService() {
        CompositionController controller = controller();
        List<CompositionExampleResponse> examples = List.of(
                new CompositionExampleResponse("Example 1", "Content 1", Language.ENGLISH, "BEGINNER", "Description 1")
        );
        when(exampleService.getAllExamples()).thenReturn(examples);

        ResponseEntity<List<CompositionExampleResponse>> result = controller.getAllCompositionExamples();

        assertEquals(200, result.getStatusCode().value());
        assertSame(examples, result.getBody());
        verify(exampleService).getAllExamples();
    }

    @Test
    void getExamplesByLanguageReturnsFilteredExamples() {
        CompositionController controller = controller();
        List<CompositionExampleResponse> examples = List.of(
                new CompositionExampleResponse("Example 1", "Content 1", Language.ENGLISH, "BEGINNER", "Description 1")
        );
        when(exampleService.getExamples(Language.ENGLISH)).thenReturn(examples);

        ResponseEntity<List<CompositionExampleResponse>> result = controller.getCompositionExamplesByLanguage("english");

        assertEquals(200, result.getStatusCode().value());
        assertSame(examples, result.getBody());
        verify(exampleService).getExamples(Language.ENGLISH);
    }

    @Test
    void getExamplesByLanguageRejectInvalidLanguage() {
        CompositionController controller = controller();

        ResponseEntity<List<CompositionExampleResponse>> result = controller.getCompositionExamplesByLanguage("invalid");

        assertEquals(400, result.getStatusCode().value());
    }

    @Test
    void getExamplesByLanguageAndLevelReturnsFilteredExamples() {
        CompositionController controller = controller();
        List<CompositionExampleResponse> examples = List.of(
                new CompositionExampleResponse("Example 1", "Content 1", Language.ENGLISH, "BEGINNER", "Description 1")
        );
        when(exampleService.getExamplesByLevel(Language.ENGLISH, "BEGINNER")).thenReturn(examples);

        ResponseEntity<List<CompositionExampleResponse>> result =
                controller.getCompositionExamplesByLanguageAndLevel("english", "BEGINNER");

        assertEquals(200, result.getStatusCode().value());
        assertSame(examples, result.getBody());
        verify(exampleService).getExamplesByLevel(Language.ENGLISH, "BEGINNER");
    }

    @Test
    void getExamplesByLanguageAndLevelRejectsInvalidLanguage() {
        CompositionController controller = controller();

        ResponseEntity<List<CompositionExampleResponse>> result =
                controller.getCompositionExamplesByLanguageAndLevel("invalid", "BEGINNER");

        assertEquals(400, result.getStatusCode().value());
    }

    private CompositionController controller() {
        return new CompositionController(commandService, queryService, exampleService);
    }

    private static CompositionResponse response() {
        return new CompositionResponse(10L, 1L, "My title", "My composition", null, null,
                CompositionStatus.DRAFT, null, null, null, null);
    }
}
