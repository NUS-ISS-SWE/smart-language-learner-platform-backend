package com.nus.iss.repository;

import com.nus.iss.model.GradingAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GradingRepository extends JpaRepository<GradingAttempt, UUID> {
    Optional<GradingAttempt> findFirstByRequestIdOrderByAttemptNumberDesc(String requestId);
}
