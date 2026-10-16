package com.nus.iss.guide;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.moderation.CategoryScores;
import org.springframework.ai.moderation.Generation;
import org.springframework.ai.moderation.Moderation;
import org.springframework.ai.moderation.ModerationModel;
import org.springframework.ai.moderation.ModerationResponse;
import org.springframework.ai.moderation.ModerationResult;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductGuideModerationServiceTest {
    @Test
    void blocksFlaggedInput() {
        ModerationModel model = mock(ModerationModel.class);
        when(model.call(any())).thenReturn(response(true, 0.01));

        ProductGuideModerationException error = assertThrows(ProductGuideModerationException.class,
                () -> service(model).moderateInput("unsafe input", "7", "learner", "request-1"));

        assertEquals(ProductGuideModerationException.Reason.BLOCKED, error.getReason());
    }

    @Test
    void allowsSafeInput() {
        ModerationModel model = mock(ModerationModel.class);
        when(model.call(any())).thenReturn(response(false, 0.01));

        assertDoesNotThrow(() -> service(model).moderateInput("Where can I update my profile?", "7", "learner", "request-2"));
    }

    @Test
    void failsClosedWhenModerationIsUnavailable() {
        ModerationModel model = mock(ModerationModel.class);
        when(model.call(any())).thenThrow(new IllegalStateException("unavailable"));

        ProductGuideModerationException error = assertThrows(ProductGuideModerationException.class,
                () -> service(model).moderateInput("hello", "7", "learner", "request-3"));

        assertEquals(ProductGuideModerationException.Reason.UNAVAILABLE, error.getReason());
    }

    private static ProductGuideModerationService service(ModerationModel model) {
        return new ProductGuideModerationService(model, new SimpleMeterRegistry(), new ObjectMapper(), "http://localhost:8086/internal/audit-events", "");
    }

    private static ModerationResponse response(boolean flagged, double violence) {
        ModerationResult result = ModerationResult.builder().flagged(flagged)
                .categoryScores(CategoryScores.builder().violence(violence).build()).build();
        return new ModerationResponse(new Generation(Moderation.builder().id("moderation-1")
                .model("omni-moderation-latest").results(List.of(result)).build()));
    }
}
