package com.nus.iss.controller;

import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.nus.iss.dto.SystemUsageMetricsResponse;
import com.nus.iss.dto.UserUsageMetricsResponse;
import com.nus.iss.service.SystemUsageMetricsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/metrics")
@RequiredArgsConstructor
public class SystemUsageMetricsController {

    private final SystemUsageMetricsService metricsService;

    @GetMapping
    public ResponseEntity<SystemUsageMetricsResponse> getMetrics(
            @RequestHeader("X-Sllp-Role") String role,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        assertAdmin(role);
        DateRange range = resolveDateRange(from, to);
        return ResponseEntity.ok(metricsService.getMetrics(range.from(), range.to()));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserUsageMetricsResponse> getUserMetrics(
            @RequestHeader("X-Sllp-Role") String role,
            @PathVariable long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        assertAdmin(role);
        DateRange range = resolveDateRange(from, to);
        return ResponseEntity.ok(metricsService.getUserMetrics(userId, range.from(), range.to()));
    }

    private static void assertAdmin(String role) {
        if (!"ADMIN".equals(role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
    }

    private static DateRange resolveDateRange(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if ((from != null && from.isAfter(today)) || (to != null && to.isAfter(today))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Metrics dates cannot be in the future");
        }
        LocalDate resolvedTo = to == null ? today : to;
        return new DateRange(from == null ? resolvedTo.minusDays(29) : from, resolvedTo);
    }

    private record DateRange(LocalDate from, LocalDate to) {
    }
}
