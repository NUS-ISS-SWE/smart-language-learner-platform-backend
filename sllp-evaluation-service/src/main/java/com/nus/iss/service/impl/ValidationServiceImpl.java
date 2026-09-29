package com.nus.iss.service.impl;

import com.nus.iss.config.SllpConstant;
import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.A2aRequestDTO;
import com.nus.iss.dto.AiGradingOutputDTO;
import com.nus.iss.dto.CriterionAssessmentDTO;
import com.nus.iss.dto.EvidenceDTO;
import com.nus.iss.dto.GradingRequestDTO;
import com.nus.iss.enums.Language;
import com.nus.iss.exception.A2aInvalidRequestException;
import com.nus.iss.exception.A2aUnauthorizedException;
import com.nus.iss.service.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValidationServiceImpl implements ValidationService {
    private final SllpProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public void validateAuthorization(String authorization) {
        String expected = properties.getA2aToken();
        String received = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7) : "";
        if (expected == null || expected.isBlank() || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), received.getBytes(StandardCharsets.UTF_8))) {
            throw new A2aUnauthorizedException();
        }
        log.info("A2A bearer authorization validated");
    }

    @Override
    public GradingRequestDTO validateAndExtract(A2aRequestDTO request) {
        String requestId = request == null || blank(request.getId()) ? UUID.randomUUID().toString() : request.getId();
        try {
            if (request == null || !SllpConstant.JSON_RPC_VERSION.equals(request.getJsonrpc())
                    || !SllpConstant.SEND_MESSAGE.equals(request.getMethod()) || request.getParams() == null
                    || request.getParams().getMessage() == null || blank(request.getParams().getMessage().getContextId())) {
                throw invalid(requestId, "unsupported or incomplete A2A request");
            }
            A2aRequestDTO.Part part = request.getParams().getMessage().getParts() == null ? null
                    : request.getParams().getMessage().getParts().stream()
                    .filter(item -> item != null && SllpConstant.JSON_MEDIA_TYPE.equals(item.getMediaType()) && item.getData() != null)
                    .findFirst().orElse(null);
            if (part == null || part.getData().getComposition() == null || part.getData().getEvidence() == null) {
                throw invalid(requestId, "missing JSON grading data");
            }
            A2aRequestDTO.Composition composition = part.getData().getComposition();
            if (composition.getUserId() == null || composition.getUserId() < 1 || blank(composition.getTitle())
                    || blank(composition.getContent()) || blank(composition.getLanguage())) {
                throw invalid(requestId, "invalid composition");
            }
            Language language;
            try {
                language = Language.valueOf(composition.getLanguage());
            } catch (IllegalArgumentException error) {
                throw invalid(requestId, "unsupported composition language");
            }
            A2aRequestDTO.RubricSnapshot snapshot = part.getData().getEvidence().getRubricSnapshot();
            validateRubric(requestId, snapshot);
            List<String> evidence = part.getData().getEvidence().getCriteria() == null ? List.of()
                    : part.getData().getEvidence().getCriteria().stream()
                    .filter(item -> item != null && item.getEvidence() != null)
                    .flatMap(item -> item.getEvidence().stream())
                    .filter(item -> item != null && !blank(item.getExcerpt()))
                    .map(A2aRequestDTO.EvidenceItem::getExcerpt).toList();
            GradingRequestDTO result = GradingRequestDTO.builder().requestId(requestId)
                    .contextId(request.getParams().getMessage().getContextId())
                    .compositionId(composition.getCompositionId()).userId(composition.getUserId())
                    .title(composition.getTitle().trim()).content(composition.getContent())
                    .language(language).rubricSnapshot(snapshot).evidence(evidence)
                    .requestHash(hash(request.getParams().getMessage().getContextId(), part.getData())).build();
            log.info("A2A request normalization completed requestId={} contextId={} rubricVersion={} criteriaCount={}",
                    result.getRequestId(), result.getContextId(), snapshot.getVersion(), snapshot.getCriteria().size());
            return result;
        } catch (A2aInvalidRequestException error) {
            throw error;
        } catch (Exception error) {
            throw new A2aInvalidRequestException(requestId, "invalid A2A grading request", error);
        }
    }

    @Override
    public void validateAiOutput(AiGradingOutputDTO output, GradingRequestDTO request) {
        if (output == null || output.getConfidence() == null || !Double.isFinite(output.getConfidence())
                || output.getConfidence() < 0 || output.getConfidence() > 1 || output.getCriteria() == null) {
            throw new IllegalStateException("AI grading returned an invalid structured result");
        }
        Set<String> expected = request.getRubricSnapshot().getCriteria().stream()
                .map(A2aRequestDTO.RubricCriterion::getName).collect(java.util.stream.Collectors.toSet());
        Set<String> actual = new HashSet<>();
        for (CriterionAssessmentDTO assessment : output.getCriteria()) {
            if (assessment == null || blank(assessment.getCriterion()) || !actual.add(assessment.getCriterion())
                    || assessment.getScore() == null || assessment.getScore().compareTo(SllpConstant.MIN_SCORE) < 0
                    || assessment.getScore().compareTo(SllpConstant.MAX_SCORE) > 0 || blank(assessment.getRationale())
                    || assessment.getEvidence() == null || assessment.getEvidence().isEmpty()) {
                throw new IllegalStateException("AI grading criteria are incomplete or invalid");
            }
            for (EvidenceDTO evidence : assessment.getEvidence()) {
                if (evidence == null || blank(evidence.getExcerpt()) || blank(evidence.getLocation())
                        || !request.getContent().contains(evidence.getExcerpt())) {
                    throw new IllegalStateException("AI grading evidence is not grounded in the composition");
                }
            }
        }
        if (!actual.equals(expected)) {
            throw new IllegalStateException("AI grading did not cover every rubric criterion exactly once");
        }
        log.info("AI structured output validated requestId={} criteriaCount={} confidence={}",
                request.getRequestId(), actual.size(), output.getConfidence());
    }

    private static void validateRubric(String requestId, A2aRequestDTO.RubricSnapshot snapshot) {
        if (snapshot == null || blank(snapshot.getVersion()) || snapshot.getCriteria() == null
                || snapshot.getCriteria().isEmpty()) throw invalid(requestId, "missing rubric snapshot");
        Set<String> names = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (A2aRequestDTO.RubricCriterion criterion : snapshot.getCriteria()) {
            if (criterion == null || blank(criterion.getName()) || blank(criterion.getGuidance())
                    || criterion.getWeight() == null || criterion.getWeight().signum() <= 0
                    || !names.add(criterion.getName())) throw invalid(requestId, "invalid rubric snapshot");
            total = total.add(criterion.getWeight());
        }
        if (total.compareTo(BigDecimal.valueOf(100)) != 0) throw invalid(requestId, "rubric weights must total 100");
    }

    private String hash(String contextId, Object value) throws Exception {
        byte[] json = (contextId + "\u0000" + objectMapper.writeValueAsString(value)).getBytes(StandardCharsets.UTF_8);
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json));
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static A2aInvalidRequestException invalid(String id, String message) {
        return new A2aInvalidRequestException(id, message);
    }
}
