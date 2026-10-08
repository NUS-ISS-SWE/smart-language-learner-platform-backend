package com.nus.iss.service.impl;

import com.nus.iss.config.SllpConstant;
import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.A2aRequestDTO;
import com.nus.iss.dto.AiGradingOutputDTO;
import com.nus.iss.dto.AiCriterionAssessmentDTO;
import com.nus.iss.dto.EvidenceDTO;
import com.nus.iss.dto.GradingRequestDTO;
import com.nus.iss.dto.WorkflowRubricCriterionDTO;
import com.nus.iss.enums.Language;
import com.nus.iss.exception.A2aInvalidRequestException;
import com.nus.iss.exception.A2aUnauthorizedException;
import com.nus.iss.service.ValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
            if (part == null || part.getData().getComposition() == null || part.getData().getEvidence() == null
                    || part.getData().getRubricSnapshot() == null) {
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
            List<String> evidence = part.getData().getEvidence().getCriteria() == null ? List.of()
                    : part.getData().getEvidence().getCriteria().stream()
                    .filter(item -> item != null && item.getEvidence() != null)
                    .flatMap(item -> item.getEvidence().stream())
                    .filter(item -> item != null && !blank(item.getExcerpt()))
                    .map(A2aRequestDTO.EvidenceItem::getExcerpt).toList();
            A2aRequestDTO.RubricSnapshot snapshot = part.getData().getRubricSnapshot();
            if (blank(snapshot.getVersion()) || snapshot.getCriteria() == null) {
                throw invalid(requestId, "invalid rubric snapshot");
            }
            List<WorkflowRubricCriterionDTO> rubric = snapshot.getCriteria().stream()
                    .filter(item -> item != null)
                    .map(item -> new WorkflowRubricCriterionDTO(item.getName(), item.getWeight(), item.getGuidance()))
                    .toList();
            validateRubric(rubric);
            GradingRequestDTO result = GradingRequestDTO.builder().requestId(requestId)
                    .contextId(request.getParams().getMessage().getContextId())
                    .compositionId(composition.getCompositionId()).userId(composition.getUserId())
                    .title(composition.getTitle().trim()).content(composition.getContent())
                    .language(language).rubricVersion(snapshot.getVersion()).rubric(rubric).evidence(evidence)
                    .requestHash(hash(request.getParams().getMessage().getContextId(), part.getData())).build();
            log.info("A2A request normalization completed requestId={} contextId={} evidenceCount={}",
                    result.getRequestId(), result.getContextId(), evidence.size());
            return result;
        } catch (A2aInvalidRequestException error) {
            throw error;
        } catch (Exception error) {
            throw new A2aInvalidRequestException(requestId, "invalid A2A grading request", error);
        }
    }

    @Override
    public void validateRubric(List<WorkflowRubricCriterionDTO> rubric) {
        if (rubric == null || rubric.isEmpty()) {
            throw new IllegalStateException("workflow rubric snapshot has no criteria");
        }
        Set<String> names = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (WorkflowRubricCriterionDTO criterion : rubric) {
            if (criterion == null || blank(criterion.name()) || blank(criterion.guidance())
                    || criterion.weight() == null || criterion.weight().signum() <= 0
                    || !names.add(criterion.name())) {
                throw new IllegalStateException("workflow rubric snapshot is invalid");
            }
            total = total.add(criterion.weight());
        }
        if (total.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new IllegalStateException("workflow rubric snapshot weights must total 100");
        }
    }

    @Override
    public void validateAiOutput(AiGradingOutputDTO output, GradingRequestDTO request, List<WorkflowRubricCriterionDTO> rubric) {
        if (output == null || output.getConfidence() == null || !Double.isFinite(output.getConfidence())
                || output.getConfidence() < 0 || output.getConfidence() > 1 || output.getCriteria() == null) {
            log.warn("AI output validation failed requestId={} reason=invalid_structured_result",
                    request.getRequestId());
            throw new IllegalStateException("AI grading returned an invalid structured result");
        }
        Set<String> expected = rubric.stream().map(WorkflowRubricCriterionDTO::name)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> actual = new HashSet<>();
        for (AiCriterionAssessmentDTO assessment : output.getCriteria()) {
            if (assessment == null || blank(assessment.getCriterion()) || !actual.add(assessment.getCriterion())
                    || assessment.getBandScore() == null
                    || assessment.getBandScore() < SllpConstant.MIN_BAND_SCORE
                    || assessment.getBandScore() > SllpConstant.MAX_BAND_SCORE
                    || blank(assessment.getRationale())
                    || assessment.getEvidence() == null || assessment.getEvidence().isEmpty()) {
                log.warn("AI output validation failed requestId={} reason=incomplete_criterion criterion={}",
                        request.getRequestId(), assessment == null ? null : assessment.getCriterion());
                throw new IllegalStateException("AI grading criteria are incomplete or invalid");
            }
            List<EvidenceDTO> groundedEvidence = assessment.getEvidence().stream()
                    .filter(evidence -> evidence != null && !blank(evidence.getExcerpt())
                            && grounded(request.getContent(), evidence.getExcerpt()))
                    .toList();
            int discardedCount = assessment.getEvidence().size() - groundedEvidence.size();
            if (groundedEvidence.isEmpty()) {
                log.warn("AI output validation failed requestId={} reason=no_grounded_evidence criterion={} returnedCount={}",
                        request.getRequestId(), assessment.getCriterion(), assessment.getEvidence().size());
                throw new IllegalStateException("AI grading evidence is not grounded in the composition");
            }
            if (discardedCount > 0) {
                log.warn("Discarded ungrounded AI evidence requestId={} criterion={} discardedCount={} groundedCount={}",
                        request.getRequestId(), assessment.getCriterion(), discardedCount, groundedEvidence.size());
                assessment.setEvidence(groundedEvidence);
            }
        }
        if (!actual.equals(expected)) {
            log.warn("AI output validation failed requestId={} reason=rubric_coverage_mismatch expectedCount={} actualCount={}",
                    request.getRequestId(), expected.size(), actual.size());
            throw new IllegalStateException("AI grading did not cover every rubric criterion exactly once");
        }
        log.info("AI structured output validated requestId={} criteriaCount={} confidence={}",
                request.getRequestId(), actual.size(), output.getConfidence());
    }

    private String hash(String contextId, Object value) throws Exception {
        byte[] json = (contextId + "\u0000" + objectMapper.writeValueAsString(value)).getBytes(StandardCharsets.UTF_8);
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json));
    }

    private static boolean grounded(String content, String excerpt) {
        String normalizedExcerpt = normalizeText(excerpt);
        return !normalizedExcerpt.isBlank() && normalizeText(content).contains(normalizedExcerpt);
    }

    private static String normalizeText(String value) {
        if (value == null) return "";
        String withoutBreakTags = value.replaceAll("(?i)<br\\s*/?>", " ");
        String withoutHtmlTags = withoutBreakTags.replaceAll("(?s)</?[A-Za-z][^>]*>", " ");
        String unescaped = HtmlUtils.htmlUnescape(withoutHtmlTags);
        return Normalizer.normalize(unescaped, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{Z}\\s]+", " ")
                .trim();
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static A2aInvalidRequestException invalid(String id, String message) {
        return new A2aInvalidRequestException(id, message);
    }
}
