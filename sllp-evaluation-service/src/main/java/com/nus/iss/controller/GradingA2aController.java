package com.nus.iss.controller;

import com.nus.iss.dto.A2aRequestDTO;
import com.nus.iss.dto.A2aResponseDTO;
import com.nus.iss.dto.GradingRequestDTO;
import com.nus.iss.dto.GradingResultDTO;
import com.nus.iss.service.GradingService;
import com.nus.iss.service.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Transport adapter for the version 2 SLLP composition-grading A2A contract. */
@RestController
@RequiredArgsConstructor
@Slf4j
public class GradingA2aController {
    private final ValidationService validationService;
    private final GradingService gradingService;

    @PostMapping("/a2a")
    public ResponseEntity<A2aResponseDTO> invoke(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                                 @RequestBody A2aRequestDTO request) {
        validationService.validateAuthorization(authorization);
        log.info("A2A grading request received requestId={} method={}",
                request == null ? null : request.getId(), request == null ? null : request.getMethod());
        GradingRequestDTO gradingRequest = validationService.validateAndExtract(request);
        log.info("A2A grading request validated requestId={} contextId={} compositionId={} userId={}",
                gradingRequest.getRequestId(), gradingRequest.getContextId(),
                gradingRequest.getCompositionId(), gradingRequest.getUserId());
        GradingResultDTO result = gradingService.grade(gradingRequest);
        log.info("A2A grading request completed requestId={} contextId={} taskId={} score={}",
                gradingRequest.getRequestId(), gradingRequest.getContextId(),
                result.getTaskId(), result.getOverallScore());
        return ResponseEntity.ok(A2aResponseDTO.completed(gradingRequest.getRequestId(), gradingRequest.getContextId(), result));
    }
}
