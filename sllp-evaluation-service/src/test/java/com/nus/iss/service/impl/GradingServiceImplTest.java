package com.nus.iss.service.impl;

import com.nus.iss.adapter.SllpAdminClient;
import com.nus.iss.adapter.SllpUserClient;
import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.AdminRubricCriterionDTO;
import com.nus.iss.dto.AiCriterionAssessmentDTO;
import com.nus.iss.dto.AiGradingOutputDTO;
import com.nus.iss.dto.AiUsage;
import com.nus.iss.dto.CriterionAssessmentDTO;
import com.nus.iss.dto.GradingRequestDTO;
import com.nus.iss.dto.GradingResultDTO;
import com.nus.iss.dto.GradingScoreResult;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.GradingAttemptStatus;
import com.nus.iss.enums.Language;
import com.nus.iss.model.GradingAttempt;
import com.nus.iss.repository.GradingRepository;
import com.nus.iss.service.AiUsageCostCalculator;
import com.nus.iss.service.ModerationService;
import com.nus.iss.service.RubricCalculator;
import com.nus.iss.service.ValidationService;
import com.nus.iss.tool.AnchorTools;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GradingServiceImplTest {
    @Test
    void returnsPersistedCompletedResultWithoutCallingOpenAi() throws Exception {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        when(builder.defaultSystem(any(Resource.class))).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);
        GradingRepository repository = mock(GradingRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        GradingResultDTO stored = GradingResultDTO.builder().overallScore(new java.math.BigDecimal("75.00"))
                .rubricScores(Map.of("TASK_IDEAS", new java.math.BigDecimal("75.00")))
                .confidence(0.9).modelVersion("model").promptVersion("grading-v3")
                .usage(AiUsage.builder().model("model").build()).build();
        GradingAttempt completed = GradingAttempt.builder().id(UUID.randomUUID()).requestId("request-1")
                .contextId("workflow-1").requestHash("hash").attemptNumber(1)
                .status(GradingAttemptStatus.COMPLETED).userId(7L).taskId("task-1")
                .artifactId("artifact-1").receivedAt(LocalDateTime.now())
                .resultPayload(objectMapper.writeValueAsString(stored)).build();
        when(repository.findFirstByRequestIdOrderByAttemptNumberDesc("request-1"))
                .thenReturn(Optional.of(completed));
        ModerationService moderationService = mock(ModerationService.class);
        GradingServiceImpl service = new GradingServiceImpl(builder, mock(ValidationService.class),
                mock(RubricCalculator.class), mock(AiUsageCostCalculator.class), repository,
                new SllpProperties(), objectMapper,
                new ClassPathResource("promptTemplates/systemPromptTemplate.st"),
                new ClassPathResource("promptTemplates/gradingPromptTemplate.st"),
                mock(SllpUserClient.class), mock(SllpAdminClient.class), mock(AnchorTools.class),
                moderationService);

        GradingResultDTO result = service.grade(GradingRequestDTO.builder()
                .requestId("request-1").contextId("workflow-1").requestHash("hash").build());

        assertEquals("task-1", result.getTaskId());
        assertEquals("artifact-1", result.getArtifactId());
        assertEquals(new java.math.BigDecimal("75.00"), result.getOverallScore());
        verifyNoInteractions(chatClient);
        verifyNoInteractions(moderationService);
    }

    @Test
    void addsUserDifficultyAndSingaporePrimaryStandardToAiPrompt() throws IOException {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        when(builder.defaultSystem(any(Resource.class))).thenReturn(builder);
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(org.mockito.ArgumentMatchers.<Consumer<ChatClient.PromptUserSpec>>any()))
                .thenReturn(requestSpec);
        when(requestSpec.tools(org.mockito.ArgumentMatchers.<Object[]>any())).thenReturn(requestSpec);
        when(requestSpec.toolContext(anyMap())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);

        @SuppressWarnings("unchecked")
        ResponseEntity<ChatResponse, AiGradingOutputDTO> response = mock(ResponseEntity.class);
        AiGradingOutputDTO output = AiGradingOutputDTO.builder()
                .criteria(List.of(AiCriterionAssessmentDTO.builder()
                        .criterion("TASK_IDEAS").bandScore(4)
                        .rationale("Clear idea.").evidence(List.of()).build()))
                .confidence(0.9)
                .build();
        ChatResponse chatResponse = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        when(chatResponse.getMetadata().getModel()).thenReturn("test-model");
        when(response.getEntity()).thenReturn(output);
        when(response.getResponse()).thenReturn(chatResponse);
        when(callResponseSpec.responseEntity(AiGradingOutputDTO.class)).thenReturn(response);

        GradingRepository repository = mock(GradingRepository.class);
        when(repository.findFirstByRequestIdOrderByAttemptNumberDesc("request-2"))
                .thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(GradingAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        SllpUserClient userClient = mock(SllpUserClient.class);
        when(userClient.getDifficultyLevel(7L)).thenReturn(DifficultyLevel.INTERMEDIATE);
        SllpAdminClient adminClient = mock(SllpAdminClient.class);
        List<AdminRubricCriterionDTO> rubric = List.of(new AdminRubricCriterionDTO(
                7L, Language.ENGLISH, DifficultyLevel.INTERMEDIATE, "TASK_IDEAS",
                "Develop ideas.", new BigDecimal("100"), "SYSTEM", "SYSTEM", null, null));
        when(adminClient.getRubricCriteria(Language.ENGLISH, DifficultyLevel.INTERMEDIATE))
                .thenReturn(rubric);
        ValidationService validationService = mock(ValidationService.class);
        RubricCalculator rubricCalculator = mock(RubricCalculator.class);
        CriterionAssessmentDTO scoredCriterion = CriterionAssessmentDTO.builder()
                .rubricCriterionId(7L).criterion("TASK_IDEAS").bandScore(4)
                .score(new BigDecimal("80.00")).rationale("Clear idea.").evidence(List.of()).build();
        when(rubricCalculator.calculate(any(), eq(rubric))).thenReturn(new GradingScoreResult(
                new BigDecimal("80.00"), Map.of("TASK_IDEAS", new BigDecimal("80.00")),
                List.of(scoredCriterion)));
        AiUsageCostCalculator usageCalculator = mock(AiUsageCostCalculator.class);
        AiUsage usage = AiUsage.builder().model("test-model").build();
        when(usageCalculator.from(any(), eq("test-model"))).thenReturn(usage);
        AnchorTools anchorTools = mock(AnchorTools.class);
        ModerationService moderationService = mock(ModerationService.class);
        GradingServiceImpl service = new GradingServiceImpl(builder, validationService,
                rubricCalculator, usageCalculator, repository, new SllpProperties(), new ObjectMapper(),
                new ClassPathResource("promptTemplates/systemPromptTemplate.st"),
                new ClassPathResource("promptTemplates/gradingPromptTemplate.st"), userClient,
                adminClient, anchorTools, moderationService);
        org.mockito.ArgumentCaptor<Resource> systemPromptCaptor =
                org.mockito.ArgumentCaptor.forClass(Resource.class);
        verify(builder).defaultSystem(systemPromptCaptor.capture());
        String systemPrompt = systemPromptCaptor.getValue().getContentAsString(StandardCharsets.UTF_8);
        assertAll(
                () -> assertTrue(systemPrompt.contains("1 - The criterion is mostly unmet.")),
                () -> assertTrue(systemPrompt.contains("2 - The criterion is partially met.")),
                () -> assertTrue(systemPrompt.contains("3 - The criterion is generally met.")),
                () -> assertTrue(systemPrompt.contains("4 - The criterion is consistently met.")),
                () -> assertTrue(systemPrompt.contains("5 - The criterion is fully and consistently met at an exceptional level for the assigned difficulty.")),
                () -> assertTrue(systemPrompt.contains("Before finalizing band scores, call getAnchorPapers")),
                () -> assertTrue(systemPrompt.contains("Treat evidence from the evidence agent as an optional reference only.")),
                () -> assertTrue(systemPrompt.contains("Base every final judgment on the original composition.")),
                () -> assertTrue(systemPrompt.contains("level-specific band")),
                () -> assertTrue(systemPrompt.contains("BEGINNER = assess against Singapore Primary 1 to 2 expectations.")),
                () -> assertTrue(systemPrompt.contains("INTERMEDIATE = assess against Singapore Primary 3 to 4 expectations.")),
                () -> assertTrue(systemPrompt.contains("ADVANCED = assess against Singapore Primary 5 to 6 expectations.")),
                () -> assertTrue(systemPrompt.contains("ADVANCED means \"grade strictly against Primary 5 to 6 expectations\"")),
                () -> assertTrue(systemPrompt.contains("Never raise a band merely because the assigned level is higher."))
        );
        String gradingPrompt = new ClassPathResource("promptTemplates/gradingPromptTemplate.st")
                .getContentAsString(StandardCharsets.UTF_8);
        assertAll(
                () -> assertTrue(gradingPrompt.contains("optional reference only")),
                () -> assertTrue(gradingPrompt.contains("do not treat it as complete or authoritative")),
                () -> assertTrue(gradingPrompt.contains("the source of truth")),
                () -> assertTrue(gradingPrompt.contains("Internal assessment-level code")),
                () -> assertTrue(gradingPrompt.contains("not a claim about the student's ability")),
                () -> assertTrue(gradingPrompt.contains("Required Singapore assessment standard"))
        );

        GradingResultDTO result = service.grade(GradingRequestDTO.builder()
                .requestId("request-2").contextId("workflow-1").requestHash("hash-2")
                .userId(7L).title("A day").content("I went home.").language(Language.ENGLISH)
                .evidence(List.of("I went home.")).build());

        verify(userClient).getDifficultyLevel(7L);
        verify(adminClient).getRubricCriteria(Language.ENGLISH, DifficultyLevel.INTERMEDIATE);
        org.mockito.InOrder moderationBeforeGrading = org.mockito.Mockito.inOrder(moderationService, chatClient);
        moderationBeforeGrading.verify(moderationService).moderateAndRecord(any(GradingRequestDTO.class));
        moderationBeforeGrading.verify(chatClient).prompt();
        assertEquals("grading-v8", result.getPromptVersion());
        assertEquals(4, result.getCriteria().getFirst().getBandScore());
        assertEquals(new BigDecimal("80.00"), result.getOverallScore());
        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Consumer<ChatClient.PromptUserSpec>> userCaptor =
                org.mockito.ArgumentCaptor.forClass(Consumer.class);
        verify(requestSpec).user(userCaptor.capture());
        ChatClient.PromptUserSpec userSpec = mock(ChatClient.PromptUserSpec.class);
        when(userSpec.text(any(Resource.class))).thenReturn(userSpec);
        when(userSpec.param(anyString(), any())).thenReturn(userSpec);
        userCaptor.getValue().accept(userSpec);
        verify(userSpec).param("difficultyLevel", "INTERMEDIATE");
        verify(userSpec).param("difficultyStandard", "Singapore Primary 3 to 4");
        verify(userSpec).param(eq("bandDescriptors"), org.mockito.ArgumentMatchers.argThat(value -> {
            String descriptors = value.toString();
            return descriptors.contains("TASK_IDEAS:")
                    && descriptors.contains("Band 1:")
                    && descriptors.contains("Band 5:")
                    && descriptors.contains("supporting details, explanations and reactions");
        }));
        verify(requestSpec).tools(anchorTools);
        verify(requestSpec).toolContext(org.mockito.ArgumentMatchers.argThat(context ->
                "request-2".equals(context.get(AnchorTools.REQUEST_ID_CONTEXT_KEY))
                        && Integer.valueOf(1).equals(context.get(AnchorTools.ATTEMPT_CONTEXT_KEY))
                        && context.containsKey(AnchorTools.TRACE_CONTEXT_KEY)));
        verify(repository, atLeastOnce()).saveAndFlush(any(GradingAttempt.class));
    }
}
