package com.nus.iss.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nus.iss.dto.ModerationRequest;
import com.nus.iss.entity.Composition;
import com.nus.iss.entity.CompositionModerationDecision;
import com.nus.iss.enums.ModerationAction;
import com.nus.iss.enums.ModerationReasonCategory;
import com.nus.iss.enums.ModerationStatus;
import com.nus.iss.exception.InvalidCompositionRequestException;
import com.nus.iss.exception.ModerationConflictException;
import com.nus.iss.repository.CompositionModerationDecisionRepository;
import com.nus.iss.repository.CompositionRepository;

@ExtendWith(MockitoExtension.class)
class CompositionModerationServiceTest {
    @Mock private CompositionRepository compositions;
    @Mock private CompositionModerationDecisionRepository decisions;

    private CompositionModerationService service;
    private Composition composition;

    @BeforeEach
    void setUp() {
        CompositionAccessService access = new CompositionAccessService(compositions);
        service = new CompositionModerationService(compositions, decisions, access, new CompositionMapper());
        composition = Composition.builder().id(4L).userId(7L).title("A draft").content("Some text")
                .moderationStatus(ModerationStatus.ACTIVE).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        when(compositions.findById(4L)).thenReturn(Optional.of(composition));
        lenient().when(compositions.save(any(Composition.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void removeRecordsDecisionAndRedactsOwnerResponse() {
        var response = service.moderate(4L, 99L, "admin", new ModerationRequest(ModerationAction.REMOVE,
                ModerationReasonCategory.PRIVACY_OR_PERSONAL_DATA, "Please remove personal details."));

        assertEquals(ModerationStatus.REMOVED, composition.getModerationStatus());
        assertEquals(ModerationReasonCategory.PRIVACY_OR_PERSONAL_DATA, composition.getModerationReasonCategory());
        assertEquals(ModerationStatus.REMOVED, response.moderation().status());
        var ownerResponse = new CompositionMapper().toOwnerResponse(composition);
        assertNull(ownerResponse.content());
        assertNull(ownerResponse.systemFeedback());
        assertEquals(true, ownerResponse.moderation().ownerRedacted());
        ArgumentCaptor<CompositionModerationDecision> captor = ArgumentCaptor.forClass(CompositionModerationDecision.class);
        verify(decisions).save(captor.capture());
        assertEquals(ModerationAction.REMOVE, captor.getValue().getAction());
        assertEquals(99L, captor.getValue().getModeratorId());
    }

    @Test
    void removeRequiresReasonAndCannotRepeat() {
        assertThrows(InvalidCompositionRequestException.class,
                () -> service.moderate(4L, 99L, "admin", new ModerationRequest(ModerationAction.REMOVE, null, null)));
        composition.setModerationStatus(ModerationStatus.REMOVED);
        assertThrows(ModerationConflictException.class,
                () -> service.moderate(4L, 99L, "admin", new ModerationRequest(ModerationAction.REMOVE,
                        ModerationReasonCategory.OTHER, null)));
    }

    @Test
    void restoreClearsCurrentReasonAndRecordsHistory() {
        composition.setModerationStatus(ModerationStatus.REMOVED);
        composition.setModerationReasonCategory(ModerationReasonCategory.OTHER);
        composition.setModerationReasonNote("Old reason");

        var response = service.moderate(4L, 99L, "admin", new ModerationRequest(ModerationAction.RESTORE, null, null));

        assertEquals(ModerationStatus.ACTIVE, composition.getModerationStatus());
        assertNull(composition.getModerationReasonCategory());
        assertNull(composition.getModerationReasonNote());
        assertEquals(ModerationStatus.ACTIVE, response.moderation().status());
        verify(decisions).save(any(CompositionModerationDecision.class));
    }
}
