package com.nus.iss.repository;

import com.nus.iss.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    List<Evaluation> findByCompositionIdOrderByEvaluatedAtDesc(Long compositionId);

    @Query("""
            SELECT e FROM Evaluation e 
            INNER JOIN Composition c ON e.compositionId = c.id 
            WHERE c.userId = :userId 
            ORDER BY e.evaluatedAt DESC
            """)
    List<Evaluation> findByUserIdOrderByEvaluatedAtDesc(@Param("userId") Long userId);
}
