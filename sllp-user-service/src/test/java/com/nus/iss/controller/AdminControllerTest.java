package com.nus.iss.controller;

import com.nus.iss.dto.AdminUserResponseDTO;
import com.nus.iss.dto.UpdateRoleRequest;
import com.nus.iss.dto.UpdateStatusRequest;
import com.nus.iss.enums.Role;
import com.nus.iss.enums.Status;
import com.nus.iss.service.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminUserService service;

    private AdminController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminController(service);
    }

    @Test
    void listUsersBuildsExpectedPageRequest() {
        Page<AdminUserResponseDTO> page = new PageImpl<>(List.of(dto()));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(service.listUsers(pageableCaptor.capture(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(page);

        ResponseEntity<Page<AdminUserResponseDTO>> response =
                controller.listUsers("ADMIN", "ming", "ACTIVE", "USER_FREE_TRIAL", 2, 15);

        assertSame(page, response.getBody());
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(2, pageable.getPageNumber());
        assertEquals(15, pageable.getPageSize());
        assertEquals("createdAt: DESC", pageable.getSort().toString());
        verify(service).listUsers(pageable, "ming", "ACTIVE", "USER_FREE_TRIAL");
    }

    @Test
    void getUserReturnsServiceResult() {
        AdminUserResponseDTO dto = dto();
        when(service.getUser(1L)).thenReturn(dto);

        ResponseEntity<AdminUserResponseDTO> response = controller.getUser("ADMIN", 1L);

        assertSame(dto, response.getBody());
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void updateStatusParsesStatusAndDelegates() {
        AdminUserResponseDTO dto = dto();
        when(service.updateUserStatus(1L, Status.SUSPENDED)).thenReturn(dto);

        ResponseEntity<AdminUserResponseDTO> response =
                controller.updateUserStatus("ADMIN", 1L, new UpdateStatusRequest("SUSPENDED"));

        assertSame(dto, response.getBody());
    }

    @Test
    void updateRoleParsesRoleAndDelegates() {
        AdminUserResponseDTO dto = dto();
        when(service.updateUserRole(1L, Role.ADMIN)).thenReturn(dto);

        ResponseEntity<AdminUserResponseDTO> response =
                controller.updateUserRole("ADMIN", 1L, new UpdateRoleRequest("ADMIN"));

        assertSame(dto, response.getBody());
    }

    @Test
    void deleteUserReturnsNoContent() {
        ResponseEntity<Void> response = controller.deleteUser("ADMIN", 1L);

        assertEquals(204, response.getStatusCode().value());
        assertNull(response.getBody());
        verify(service).softDeleteUser(1L);
    }

    @Test
    void everyEndpointRejectsNonAdminRoleBeforeCallingService() {
        assertForbidden(() -> controller.listUsers("USER_FREE_TRIAL", null, null, null, 0, 20));
        assertForbidden(() -> controller.getUser(null, 1L));
        assertForbidden(() -> controller.updateUserStatus("admin", 1L, new UpdateStatusRequest("ACTIVE")));
        assertForbidden(() -> controller.updateUserRole("", 1L, new UpdateRoleRequest("ADMIN")));
        assertForbidden(() -> controller.deleteUser("USER_PAID", 1L));
        verifyNoInteractions(service);
    }

    @Test
    void invalidStatusAndRoleAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.updateUserStatus("ADMIN", 1L, new UpdateStatusRequest("active")));
        assertThrows(IllegalArgumentException.class,
                () -> controller.updateUserRole("ADMIN", 1L, new UpdateRoleRequest("unknown")));
    }

    private static void assertForbidden(Runnable invocation) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, invocation::run);
        assertEquals(403, error.getStatusCode().value());
    }

    private static AdminUserResponseDTO dto() {
        return new AdminUserResponseDTO(1L, "ming", "ming@example.com", "ADMIN", "ACTIVE",
                "ENGLISH", "BEGINNER", 5000L, 0L, null, null);
    }
}
