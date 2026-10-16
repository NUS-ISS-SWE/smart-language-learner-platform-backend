package com.nus.iss.service;

import com.nus.iss.dto.GradingRequestDTO;
import com.nus.iss.dto.AiGradingOutputDTO;

public interface ModerationService {
    void moderateComposition(GradingRequestDTO request);

    void moderateFeedback(GradingRequestDTO request, AiGradingOutputDTO output);
}
