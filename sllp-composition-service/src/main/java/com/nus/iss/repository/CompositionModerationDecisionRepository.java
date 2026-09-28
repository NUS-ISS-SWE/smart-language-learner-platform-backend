package com.nus.iss.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nus.iss.entity.CompositionModerationDecision;

public interface CompositionModerationDecisionRepository extends JpaRepository<CompositionModerationDecision, UUID> {
}
