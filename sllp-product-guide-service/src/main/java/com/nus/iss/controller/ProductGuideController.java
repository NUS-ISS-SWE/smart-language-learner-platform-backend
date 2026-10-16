package com.nus.iss.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import com.nus.iss.guide.ProductGuideModerationService;

@RestController
@RequestMapping("/api/v1/product-guide")
public class ProductGuideController {

    private final ChatClient chatClient;
    private final ProductGuideModerationService moderationService;

    public ProductGuideController(ChatClient chatClient, ProductGuideModerationService moderationService) {
        this.chatClient = chatClient;
        this.moderationService = moderationService;
    }

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestHeader("X-Sllp-Username") String username,
                             @RequestHeader(value = "X-Sllp-User-Id", required = false) String userId,
                             @RequestHeader(value = "X-Sllp-Request-Id", required = false) String requestId,
                             @RequestParam String message) {
        moderationService.moderateInput(message, userId, username, requestId);
        long startedAt = System.nanoTime();
        return chatClient.prompt()
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, username))
                .stream()
                .content()
                .doOnComplete(() -> moderationService.recordGeneration("SUCCESS", userId, username, requestId,
                        (System.nanoTime() - startedAt) / 1_000_000))
                .doOnError(error -> moderationService.recordGeneration("FAILURE", userId, username, requestId,
                        (System.nanoTime() - startedAt) / 1_000_000));
    }
}
