package com.nus.iss.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nus.iss.dto.AdminUserResponseDTO;
import com.nus.iss.entity.AppUser;
import com.nus.iss.enums.Role;
import com.nus.iss.enums.Status;
import com.nus.iss.repository.AppUserRepository;
import com.nus.iss.service.AdminUserService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final AppUserRepository appUserRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponseDTO> listUsers(Pageable pageable, String search, String status, String role) {
        Specification<AppUser> spec = (root, query, cb) -> cb.conjunction();

        if (search != null && !search.isBlank()) {
            final String pattern = "%" + search.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("username")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern)
            ));
        }

        if (status != null && !status.isBlank()) {
            final Status statusEnum = Status.valueOf(status);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), statusEnum));
        }

        if (role != null && !role.isBlank()) {
            final Role roleEnum = Role.valueOf(role);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), roleEnum));
        }

        return appUserRepository.findAll(spec, pageable).map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponseDTO getUser(Long id) {
        return toDTO(findUserById(id));
    }

    @Override
    @Transactional
    public AdminUserResponseDTO updateUserStatus(Long id, Status newStatus) {
        log.info("Updating status for user {} to {}", id, newStatus);
        AppUser user = findUserById(id);
        user.setStatus(newStatus);
        return toDTO(appUserRepository.save(user));
    }

    @Override
    @Transactional
    public AdminUserResponseDTO updateUserRole(Long id, Role newRole) {
        log.info("Updating role for user {} to {}", id, newRole);
        AppUser user = findUserById(id);
        user.setRole(newRole);
        return toDTO(appUserRepository.save(user));
    }

    @Override
    @Transactional
    public void softDeleteUser(Long id) {
        log.info("Soft-deleting user {}", id);
        AppUser user = findUserById(id);
        user.setDeletedAt(LocalDateTime.now(ZoneOffset.UTC));
        appUserRepository.save(user);
    }

    private AppUser findUserById(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    private AdminUserResponseDTO toDTO(AppUser user) {
        return new AdminUserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getStatus() != null ? user.getStatus().name() : null,
                user.getPreferredLanguage() != null ? user.getPreferredLanguage().name() : null,
                user.getDifficultyLevel() != null ? user.getDifficultyLevel().name() : null,
                user.getAiTokenBalance(),
                user.getAiTokenUsed(),
                user.getCreatedAt(),
                user.getDeletedAt()
        );
    }
}
