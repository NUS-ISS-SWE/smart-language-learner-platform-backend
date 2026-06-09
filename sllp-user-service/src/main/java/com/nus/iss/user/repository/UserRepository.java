package com.nus.iss.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nus.iss.user.entity.User;

/**
 * Repository for User entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    /**
     * Find user by email.
     *
     * @param email user email
     * @return optional containing user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if user exists by email.
     *
     * @param email user email
     * @return true if user exists
     */
    boolean existsByEmail(String email);
}
