package com.nus.iss.service;

import com.nus.iss.dto.GradingRequestDTO;

public interface SafeguardService {
    void screenAndRecord(GradingRequestDTO request);
}
