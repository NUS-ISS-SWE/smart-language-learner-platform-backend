package com.nus.iss.service;


import com.nus.iss.dto.CompositionDTO;
import com.nus.iss.dto.CompositionGenerationCompletedEvent;
import com.nus.iss.dto.CompositionGradingCompletedEvent;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CompositionService {

    CompositionDTO createNewComposition(String username, CompositionDTO compositionDTO);

    List<CompositionDTO> getAllCompositionByUsername(String username, Pageable pageable);

    CompositionDTO getSingleCompositionById(String username, Long id);

    CompositionDTO updateComposition(String username, Long id, CompositionDTO compositionDTO);

    void deleteComposition(String username, Long id);

    CompositionDTO requestToGradeComposition(String username, Long id);

    void completeCompositionGrading(CompositionGradingCompletedEvent event);

    CompositionDTO generateComposition(String username, CompositionDTO compositionDTO);

    void completeCompositionGeneration(CompositionGenerationCompletedEvent event);
}
