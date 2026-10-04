package com.nus.iss.service.impl;

import com.nus.iss.dto.AdminUserResponseDTO;
import com.nus.iss.entity.AppUser;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.PreferredLanguage;
import com.nus.iss.enums.Role;
import com.nus.iss.enums.Status;
import com.nus.iss.repository.AppUserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private AppUserRepository repository;

    @Test
    void listUsersWithoutFiltersReturnsMappedPage() {
        AdminUserServiceImpl service = new AdminUserServiceImpl(repository);
        Pageable pageable = PageRequest.of(0, 20);
        AppUser user = user();
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(user), pageable, 1));

        Page<AdminUserResponseDTO> result = service.listUsers(pageable, null, " ", null);

        assertEquals(1, result.getTotalElements());
        assertEquals("ming", result.getContent().getFirst().username());
        assertEquals("ACTIVE", result.getContent().getFirst().status());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void listUsersWithAllFiltersBuildsAndExecutesSpecification() {
        AdminUserServiceImpl service = new AdminUserServiceImpl(repository);
        Pageable pageable = PageRequest.of(1, 5);
        Root<AppUser> root = org.mockito.Mockito.mock(Root.class);
        CriteriaQuery<?> query = org.mockito.Mockito.mock(CriteriaQuery.class);
        CriteriaBuilder builder = org.mockito.Mockito.mock(CriteriaBuilder.class);
        Path username = org.mockito.Mockito.mock(Path.class);
        Path email = org.mockito.Mockito.mock(Path.class);
        Path status = org.mockito.Mockito.mock(Path.class);
        Path role = org.mockito.Mockito.mock(Path.class);
        Expression loweredUsername = org.mockito.Mockito.mock(Expression.class);
        Expression loweredEmail = org.mockito.Mockito.mock(Expression.class);
        Predicate predicate = org.mockito.Mockito.mock(Predicate.class);

        when(root.get("username")).thenReturn(username);
        when(root.get("email")).thenReturn(email);
        when(root.get("status")).thenReturn(status);
        when(root.get("role")).thenReturn(role);
        when(builder.lower(username)).thenReturn(loweredUsername);
        when(builder.lower(email)).thenReturn(loweredEmail);
        when(builder.conjunction()).thenReturn(predicate);
        when(builder.like(any(Expression.class), eq("%ming%"))).thenReturn(predicate);
        when(builder.or(any(Predicate.class), any(Predicate.class))).thenReturn(predicate);
        doReturn(predicate).when(builder).equal(any(Expression.class), any(Object.class));
        when(builder.and(any(Predicate.class), any(Predicate.class))).thenReturn(predicate);
        when(repository.findAll(any(Specification.class), eq(pageable))).thenAnswer(invocation -> {
            Specification<AppUser> specification = invocation.getArgument(0);
            specification.toPredicate(root, query, builder);
            return new PageImpl<>(List.of(user()), pageable, 1);
        });

        Page<AdminUserResponseDTO> result = service.listUsers(pageable, "MiNg", "ACTIVE", "ADMIN");

        assertEquals(1, result.getNumberOfElements());
        verify(builder).like(loweredUsername, "%ming%");
        verify(builder).like(loweredEmail, "%ming%");
        verify(builder).equal(status, Status.ACTIVE);
        verify(builder).equal(role, Role.ADMIN);
    }

    @Test
    void listUsersRejectsInvalidStatusAndRole() {
        AdminUserServiceImpl service = new AdminUserServiceImpl(repository);
        Pageable pageable = PageRequest.of(0, 10);

        assertThrows(IllegalArgumentException.class,
                () -> service.listUsers(pageable, null, "INVALID", null));
        assertThrows(IllegalArgumentException.class,
                () -> service.listUsers(pageable, null, null, "INVALID"));
    }

    @Test
    void getUserReturnsMappedUser() {
        AdminUserServiceImpl service = new AdminUserServiceImpl(repository);
        when(repository.findById(1L)).thenReturn(Optional.of(user()));

        AdminUserResponseDTO result = service.getUser(1L);

        assertEquals(1L, result.id());
        assertEquals("USER_FREE_TRIAL", result.role());
        assertEquals("ENGLISH", result.preferredLanguage());
        assertEquals("BEGINNER", result.difficultyLevel());
    }

    @Test
    void getUserMapsNullableEnums() {
        AdminUserServiceImpl service = new AdminUserServiceImpl(repository);
        AppUser user = AppUser.builder().id(2L).username("plain").email("plain@example.com").build();
        when(repository.findById(2L)).thenReturn(Optional.of(user));

        AdminUserResponseDTO result = service.getUser(2L);

        assertEquals(null, result.role());
        assertEquals(null, result.status());
        assertEquals(null, result.preferredLanguage());
        assertEquals(null, result.difficultyLevel());
    }

    @Test
    void getUserWhenMissingThrowsEntityNotFound() {
        AdminUserServiceImpl service = new AdminUserServiceImpl(repository);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException error = assertThrows(EntityNotFoundException.class,
                () -> service.getUser(99L));

        assertEquals("User not found with id: 99", error.getMessage());
    }

    @Test
    void updateStatusChangesAndSavesUser() {
        AdminUserServiceImpl service = new AdminUserServiceImpl(repository);
        AppUser user = user();
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(repository.save(user)).thenReturn(user);

        AdminUserResponseDTO result = service.updateUserStatus(1L, Status.SUSPENDED);

        assertEquals(Status.SUSPENDED, user.getStatus());
        assertEquals("SUSPENDED", result.status());
        verify(repository).save(user);
    }

    @Test
    void updateRoleChangesAndSavesUser() {
        AdminUserServiceImpl service = new AdminUserServiceImpl(repository);
        AppUser user = user();
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(repository.save(user)).thenReturn(user);

        AdminUserResponseDTO result = service.updateUserRole(1L, Role.ADMIN);

        assertEquals(Role.ADMIN, user.getRole());
        assertEquals("ADMIN", result.role());
    }

    @Test
    void softDeleteSetsUtcTimestampAndSavesUser() {
        AdminUserServiceImpl service = new AdminUserServiceImpl(repository);
        AppUser user = user();
        when(repository.findById(1L)).thenReturn(Optional.of(user));

        service.softDeleteUser(1L);

        assertNotNull(user.getDeletedAt());
        assertTrue(user.getDeletedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        verify(repository).save(user);
    }

    private static AppUser user() {
        return AppUser.builder()
                .id(1L)
                .username("ming")
                .email("ming@example.com")
                .role(Role.USER_FREE_TRIAL)
                .status(Status.ACTIVE)
                .preferredLanguage(PreferredLanguage.ENGLISH)
                .difficultyLevel(DifficultyLevel.BEGINNER)
                .aiTokenBalance(5000L)
                .aiTokenUsed(20L)
                .createdAt(LocalDateTime.of(2026, 1, 2, 3, 4))
                .build();
    }
}
