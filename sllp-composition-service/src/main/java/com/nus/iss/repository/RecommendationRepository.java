package com.nus.iss.repository;

import com.nus.iss.entity.Recommendation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Recommendation> findByUserIdOrderByCreatedAtDesc(Long userId);
}
