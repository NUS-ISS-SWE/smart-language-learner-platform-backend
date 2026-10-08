package com.nus.iss.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.nus.iss.entity.AppUserRef;

@Repository
public interface AppUserRoleRepository extends JpaRepository<AppUserRef, Long> {

    /** Serializes quota reservations for one learner without introducing a global quota lock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM AppUserRef u WHERE u.id = :userId")
    java.util.Optional<AppUserRef> findByIdForUpdate(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE AppUserRef u SET u.role = :role WHERE u.id = :userId")
    int updateRole(@Param("userId") Long userId, @Param("role") String role);
}
