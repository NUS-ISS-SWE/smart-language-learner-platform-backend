package com.nus.iss.service;

import com.nus.iss.dto.A2aRequestDTO;
import com.nus.iss.dto.AiGradingOutputDTO;
import com.nus.iss.dto.GradingRequestDTO;

public interface ValidationService {
    void validateAuthorization(String authorization);
    GradingRequestDTO validateAndExtract(A2aRequestDTO request);
    void validateAiOutput(AiGradingOutputDTO output, GradingRequestDTO request);
}
