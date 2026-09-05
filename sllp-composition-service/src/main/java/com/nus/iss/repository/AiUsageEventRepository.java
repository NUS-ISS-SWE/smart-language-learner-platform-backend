package com.nus.iss.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nus.iss.entity.AiUsageEvent;

@Repository
public interface AiUsageEventRepository extends JpaRepository<AiUsageEvent, Long> {
}
