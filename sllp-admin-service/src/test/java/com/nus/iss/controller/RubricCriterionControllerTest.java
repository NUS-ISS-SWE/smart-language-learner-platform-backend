package com.nus.iss.controller;

import com.nus.iss.dto.RubricCriterionRequest;
import com.nus.iss.dto.RubricCriterionResponse;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import com.nus.iss.service.RubricCriterionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RubricCriterionControllerTest {

    @Mock
    private RubricCriterionService service;

    @Test
    void createReturnsCreatedResponseAndResourceLocation() {
        RubricCriterionController controller = new RubricCriterionController(service);
        RubricCriterionRequest request = request();
        RubricCriterionResponse created = response();
        when(service.create(request, "alextan")).thenReturn(created);

        ResponseEntity<RubricCriterionResponse> response = controller.create("alextan", request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("/api/v1/admin/rubrics/7", response.getHeaders().getLocation().toString());
        assertSame(created, response.getBody());
    }

    @Test
    void getAllPassesOptionalFiltersToService() {
        RubricCriterionController controller = new RubricCriterionController(service);
        List<RubricCriterionResponse> criteria = List.of(response());
        when(service.getAll(Language.ENGLISH, DifficultyLevel.BEGINNER)).thenReturn(criteria);

        ResponseEntity<List<RubricCriterionResponse>> response =
                controller.getAll(Language.ENGLISH, DifficultyLevel.BEGINNER);

        assertEquals(200, response.getStatusCode().value());
        assertSame(criteria, response.getBody());
    }

    @Test
    void getByIdReturnsCriterion() {
        RubricCriterionController controller = new RubricCriterionController(service);
        RubricCriterionResponse criterion = response();
        when(service.getById(7L)).thenReturn(criterion);

        ResponseEntity<RubricCriterionResponse> response = controller.getById(7L);

        assertEquals(200, response.getStatusCode().value());
        assertSame(criterion, response.getBody());
    }

    @Test
    void updateReturnsUpdatedCriterion() {
        RubricCriterionController controller = new RubricCriterionController(service);
        RubricCriterionRequest request = request();
        RubricCriterionResponse updated = response();
        when(service.update(7L, request, "alextan")).thenReturn(updated);

        ResponseEntity<RubricCriterionResponse> response = controller.update(7L, "alextan", request);

        assertEquals(200, response.getStatusCode().value());
        assertSame(updated, response.getBody());
    }

    @Test
    void deleteReturnsNoContent() {
        RubricCriterionController controller = new RubricCriterionController(service);

        ResponseEntity<Void> response = controller.delete(7L);

        assertEquals(204, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(service).delete(7L);
    }

    private static RubricCriterionRequest request() {
        return new RubricCriterionRequest(
                Language.ENGLISH,
                DifficultyLevel.BEGINNER,
                "TASK_IDEAS",
                "Assess relevant ideas.",
                new BigDecimal("25.00"));
    }

    private static RubricCriterionResponse response() {
        return new RubricCriterionResponse(
                7L,
                Language.ENGLISH,
                DifficultyLevel.BEGINNER,
                "TASK_IDEAS",
                "Assess relevant ideas.",
                new BigDecimal("25.00"),
                "alextan",
                "alextan",
                null,
                null);
    }
}
