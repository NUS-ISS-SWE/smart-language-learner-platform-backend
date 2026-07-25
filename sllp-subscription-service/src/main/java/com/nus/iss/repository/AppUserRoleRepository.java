package com.nus.iss.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.nus.iss.entity.AppUserRef;

@Repository
public interface AppUserRoleRepository extends JpaRepository<AppUserRef, Long> {

    @Modifying
    @Transactional
    @Query("UPDATE AppUserRef u SET u.role = :role WHERE u.id = :userId")
    int updateRole(@Param("userId") Long userId, @Param("role") String role);
}
