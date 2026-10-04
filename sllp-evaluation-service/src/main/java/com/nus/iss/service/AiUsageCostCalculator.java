package com.nus.iss.service;

import com.nus.iss.dto.AiUsage;
import org.springframework.ai.chat.metadata.Usage;

public interface AiUsageCostCalculator {
    AiUsage from(Usage usage, String responseModel);
}
