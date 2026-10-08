package com.nus.iss.repository;

import com.nus.iss.entity.SuggestionFeedback;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuggestionFeedbackRepository extends JpaRepository<SuggestionFeedback, Long> {
    Optional<SuggestionFeedback> findByCompositionIdAndUserIdAndSuggestionIndex(Long compositionId, Long userId, Integer suggestionIndex);
}
