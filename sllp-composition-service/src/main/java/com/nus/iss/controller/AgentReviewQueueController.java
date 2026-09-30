package com.nus.iss.controller;

import com.nus.iss.dto.AgentReviewQueueResponse;
import com.nus.iss.service.AgentReviewQueueService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/v1/admin/agent-review-queue") @RequiredArgsConstructor
public class AgentReviewQueueController {
    private final AgentReviewQueueService queue;

    @GetMapping
    public List<AgentReviewQueueResponse> list(@RequestHeader("X-Sllp-Role") String role) {
        if (!"ADMIN".equals(role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        return queue.openItems();
    }
}
