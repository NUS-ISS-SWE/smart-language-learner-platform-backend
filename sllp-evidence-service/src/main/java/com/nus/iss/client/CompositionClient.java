package com.nus.iss.client;

import com.nus.iss.dto.CompositionDTO;

public interface CompositionClient {
    CompositionDTO getComposition(Long userId, Long compositionId);
}
