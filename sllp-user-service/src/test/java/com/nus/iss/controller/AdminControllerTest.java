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
import com.nus.iss.service.AdminUserService;
import com.nus.iss.service.UsageMetricsService;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminUserService adminUserService;

    @Mock
    private UsageMetricsService usageMetricsService;

    @Test
    void metricsAreReturnedForAnAdmin() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 2);
        SystemUsageMetricsResponse metrics = new SystemUsageMetricsResponse(from, to,
                new SystemUsageMetricsResponse.Summary(1, 1, 0, 0, 0, 0, 0), List.of());
        when(usageMetricsService.getMetrics(from, to)).thenReturn(metrics);

        ResponseEntity<SystemUsageMetricsResponse> response = new AdminController(adminUserService, usageMetricsService)
                .getUsageMetrics("ADMIN", from, to);

        assertEquals(200, response.getStatusCode().value());
        assertSame(metrics, response.getBody());
        verify(usageMetricsService).getMetrics(from, to);
    }

    @Test
    void nonAdminsCannotReadMetrics() {
        AdminController controller = new AdminController(adminUserService, usageMetricsService);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.getUsageMetrics("USER_BASIC", LocalDate.now(), LocalDate.now()));

        assertEquals(403, exception.getStatusCode().value());
    }
}
