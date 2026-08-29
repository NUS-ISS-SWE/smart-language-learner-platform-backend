package com.nus.iss.controller;

import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.nus.iss.dto.AdminUserResponseDTO;
import com.nus.iss.dto.SystemUsageMetricsResponse;
import com.nus.iss.dto.UpdateRoleRequest;
import com.nus.iss.dto.UpdateStatusRequest;
import com.nus.iss.enums.Role;
import com.nus.iss.enums.Status;
import com.nus.iss.service.AdminUserService;
import com.nus.iss.service.UsageMetricsService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminUserService adminUserService;
    private final UsageMetricsService usageMetricsService;

    @GetMapping("/metrics")
    public ResponseEntity<SystemUsageMetricsResponse> getUsageMetrics(
            @RequestHeader("X-Sllp-Role") String role,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        assertAdmin(role);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate resolvedTo = to == null ? today : to;
        LocalDate resolvedFrom = from == null ? resolvedTo.minusDays(29) : from;
        return ResponseEntity.ok(usageMetricsService.getMetrics(resolvedFrom, resolvedTo));
    }

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserResponseDTO>> listUsers(
            @RequestHeader("X-Sllp-Role") String role,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userRole,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        assertAdmin(role);
        log.info("Admin listing users — search={}, status={}, role={}", search, status, userRole);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(adminUserService.listUsers(pageable, search, status, userRole));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserResponseDTO> getUser(
            @RequestHeader("X-Sllp-Role") String role,
            @PathVariable Long id) {
        assertAdmin(role);
        log.info("Admin fetching user {}", id);
        return ResponseEntity.ok(adminUserService.getUser(id));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<AdminUserResponseDTO> updateUserStatus(
            @RequestHeader("X-Sllp-Role") String role,
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {
        assertAdmin(role);
        Status newStatus = Status.valueOf(request.status());
        log.info("Admin updating status for user {} to {}", id, newStatus);
        return ResponseEntity.ok(adminUserService.updateUserStatus(id, newStatus));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<AdminUserResponseDTO> updateUserRole(
            @RequestHeader("X-Sllp-Role") String role,
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request) {
        assertAdmin(role);
        Role newRole = Role.valueOf(request.role());
        log.info("Admin updating role for user {} to {}", id, newRole);
        return ResponseEntity.ok(adminUserService.updateUserRole(id, newRole));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(
            @RequestHeader("X-Sllp-Role") String role,
            @PathVariable Long id) {
        assertAdmin(role);
        log.info("Admin soft-deleting user {}", id);
        adminUserService.softDeleteUser(id);
        return ResponseEntity.noContent().build();
    }

    private static void assertAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
