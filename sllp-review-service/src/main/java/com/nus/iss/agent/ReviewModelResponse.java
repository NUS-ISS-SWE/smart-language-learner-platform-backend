package com.nus.iss.agent;

import java.util.List;

public record ReviewModelResponse(String decision, double confidence, List<Finding> findings) {
    public record Finding(String criterion, String message) {}
}
