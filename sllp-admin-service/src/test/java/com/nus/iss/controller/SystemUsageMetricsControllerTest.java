package com.nus.iss.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import com.nus.iss.dto.SystemUsageMetricsResponse;
import com.nus.iss.dto.UserUsageMetricsResponse;
import com.nus.iss.service.SystemUsageMetricsService;

@ExtendWith(MockitoExtension.class)
class SystemUsageMetricsControllerTest {

    @Mock
    private SystemUsageMetricsService service;

    @Test
    void returnsMetricsForAnAdmin() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 2);
        SystemUsageMetricsResponse metrics = new SystemUsageMetricsResponse(from, to,
                new SystemUsageMetricsResponse.Summary(1, 1, 0, 0, 0, 0, 0, 0, null, 0, 0, 0, 0, 0, 0, 0), List.of(), List.of());
        when(service.getMetrics(from, to)).thenReturn(metrics);

        ResponseEntity<SystemUsageMetricsResponse> response = new SystemUsageMetricsController(service)
                .getMetrics("ADMIN", from, to);

        assertEquals(200, response.getStatusCode().value());
        assertSame(metrics, response.getBody());
        verify(service).getMetrics(from, to);
    }

    @Test
    void rejectsNonAdminRequests() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> new SystemUsageMetricsController(service).getMetrics("USER_BASIC", LocalDate.now(), LocalDate.now()));

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void returnsUsageForAnIndividualUserToAnAdmin() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 2);
        UserUsageMetricsResponse metrics = new UserUsageMetricsResponse(42L, from, to,
                new UserUsageMetricsResponse.Summary(1, 2, 3, 2, 1, 4, 3, 1, 5, 6, null), List.of());
        when(service.getUserMetrics(42L, from, to)).thenReturn(metrics);

        ResponseEntity<UserUsageMetricsResponse> response = new SystemUsageMetricsController(service)
                .getUserMetrics("ADMIN", 42L, from, to);

        assertEquals(200, response.getStatusCode().value());
        assertSame(metrics, response.getBody());
        verify(service).getUserMetrics(42L, from, to);
    }
}
