package com.nus.iss.repository;

import com.nus.iss.model.CompositionModerationFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompositionModerationFlagRepository extends JpaRepository<CompositionModerationFlag, UUID> {
}
