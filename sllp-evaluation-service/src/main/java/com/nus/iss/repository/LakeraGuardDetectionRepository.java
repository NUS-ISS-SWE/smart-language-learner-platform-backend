package com.nus.iss.repository;

import com.nus.iss.model.LakeraGuardDetection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LakeraGuardDetectionRepository extends JpaRepository<LakeraGuardDetection, UUID> {
}
