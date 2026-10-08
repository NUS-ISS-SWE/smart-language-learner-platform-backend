package com.nus.iss.controller;

import com.nus.iss.dto.SuggestionFeedbackRequest;
import com.nus.iss.dto.SuggestionFeedbackResponse;
import com.nus.iss.service.SuggestionFeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/composition") @RequiredArgsConstructor
public class SuggestionFeedbackController {
    private final SuggestionFeedbackService service;

    @PostMapping("/{compositionId}/suggestions/{suggestionIndex}/feedback")
    public SuggestionFeedbackResponse submit(@RequestHeader("X-Sllp-User-Id") Long userId,
                                             @PathVariable Long compositionId, @PathVariable int suggestionIndex,
                                             @Valid @RequestBody SuggestionFeedbackRequest request) {
        return service.upsert(userId, compositionId, suggestionIndex, request);
    }
}
