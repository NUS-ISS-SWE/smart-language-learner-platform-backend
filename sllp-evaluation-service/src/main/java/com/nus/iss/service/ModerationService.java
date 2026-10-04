package com.nus.iss.service;

import com.nus.iss.dto.GradingRequestDTO;

public interface ModerationService {
    void moderateAndRecord(GradingRequestDTO request);
}
