package com.nus.iss.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.nus.iss.dto.AdminUserResponseDTO;
import com.nus.iss.enums.Role;
import com.nus.iss.enums.Status;

public interface AdminUserService {

    Page<AdminUserResponseDTO> listUsers(Pageable pageable, String search, String status, String role);

    AdminUserResponseDTO getUser(Long id);

    AdminUserResponseDTO updateUserStatus(Long id, Status newStatus);

    AdminUserResponseDTO updateUserRole(Long id, Role newRole);

    void softDeleteUser(Long id);
}
