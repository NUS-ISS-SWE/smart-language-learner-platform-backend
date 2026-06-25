package com.nus.iss.repository;

import com.nus.iss.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    List<Evaluation> findByCompositionIdOrderByEvaluatedAtDesc(Long compositionId);
}
