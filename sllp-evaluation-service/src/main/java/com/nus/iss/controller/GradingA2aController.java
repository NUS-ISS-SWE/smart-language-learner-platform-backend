package com.nus.iss.controller;

import com.nus.iss.dto.A2aRequestDTO;
import com.nus.iss.dto.A2aResponseDTO;
import com.nus.iss.dto.GradingRequestDTO;
import com.nus.iss.dto.GradingResultDTO;
import com.nus.iss.service.GradingService;
import com.nus.iss.service.ValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Transport adapter for the version 2 SLLP composition-grading A2A contract. */
@RestController
@RequiredArgsConstructor
public class GradingA2aController {
    private final ValidationService validationService;
    private final GradingService gradingService;

    @PostMapping("/a2a")
    public ResponseEntity<A2aResponseDTO> invoke(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                                 @RequestBody A2aRequestDTO request) {
        validationService.validateAuthorization(authorization);
        GradingRequestDTO gradingRequest = validationService.validateAndExtract(request);
        GradingResultDTO result = gradingService.grade(gradingRequest);
        return ResponseEntity.ok(A2aResponseDTO.completed(gradingRequest.getRequestId(), gradingRequest.getContextId(), result));
    }
}
