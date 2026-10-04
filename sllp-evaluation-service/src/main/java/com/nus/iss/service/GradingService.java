package com.nus.iss.service;

import com.nus.iss.dto.GradingRequestDTO;
import com.nus.iss.dto.GradingResultDTO;

public interface GradingService {
    GradingResultDTO grade(GradingRequestDTO request);
}
