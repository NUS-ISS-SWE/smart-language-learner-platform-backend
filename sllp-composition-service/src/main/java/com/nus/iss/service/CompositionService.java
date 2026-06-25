package com.nus.iss.service;


import com.nus.iss.dto.CompositionDTO;
import com.nus.iss.dto.CompositionGenerationCompletedEvent;
import com.nus.iss.dto.CompositionGradingCompletedEvent;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CompositionService {

    CompositionDTO createNewComposition(Long userId, CompositionDTO compositionDTO);

    List<CompositionDTO> getAllCompositionByUserId(Long userId, Pageable pageable);

    CompositionDTO getSingleCompositionById(Long userId, Long id);

    CompositionDTO updateComposition(Long userId, Long id, CompositionDTO compositionDTO);

    void deleteComposition(Long userId, Long id);

    CompositionDTO requestToGradeComposition(Long userId, Long id);

    void completeCompositionGrading(CompositionGradingCompletedEvent event);

    CompositionDTO generateComposition(Long userId, CompositionDTO compositionDTO);

    void completeCompositionGeneration(CompositionGenerationCompletedEvent event);
}
