package com.nus.iss.service.impl;

import com.nus.iss.agent.EvidenceAgentRequest;
import com.nus.iss.agent.EvidenceAgentService;
import com.nus.iss.client.CompositionClient;
import com.nus.iss.dto.CompositionDTO;
import com.nus.iss.dto.EvidenceCollectionResult;
import com.nus.iss.enums.Language;
import com.nus.iss.exception.EvidenceGenerationException;
import com.nus.iss.memory.EvidenceMemoryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvidenceCollectionServiceImplTest {

    @Mock
    private CompositionClient compositionClient;

    @Mock
    private EvidenceAgentService evidenceAgentService;

    private final EvidenceMemoryStore memoryStore = new EvidenceMemoryStore();

    private EvidenceCollectionServiceImpl service() {
        return new EvidenceCollectionServiceImpl(compositionClient, evidenceAgentService, memoryStore);
    }

    @Test
    void collectsEvidenceForTheFetchedCompositionAndRemembersIt() {
        when(compositionClient.getComposition(7L, 12L))
                .thenReturn(new CompositionDTO(12L, 7L, "My holiday", "I went to Penang.", Language.ENGLISH));
        EvidenceCollectionResult result = result(12L);
        when(evidenceAgentService.collect(any(EvidenceAgentRequest.class), anyString())).thenReturn(result);

        EvidenceCollectionResult returned = service().collect(7L, 12L);

        assertSame(result, returned);
        ArgumentCaptor<EvidenceAgentRequest> request = ArgumentCaptor.forClass(EvidenceAgentRequest.class);
        ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
        verify(evidenceAgentService).collect(request.capture(), requestId.capture());
        assertEquals(new EvidenceAgentRequest(12L, 7L, "My holiday", "I went to Penang.", Language.ENGLISH), request.getValue());
        assertNotNull(requestId.getValue());
        assertEquals(Optional.of(result), service().getLast(12L));
    }

    @Test
    void compositionWithoutContentIsRejectedBeforeCallingTheModel() {
        when(compositionClient.getComposition(7L, 12L))
                .thenReturn(new CompositionDTO(12L, 7L, "Empty", "   ", Language.ENGLISH));

        assertThrows(EvidenceGenerationException.class, () -> service().collect(7L, 12L));
        verifyNoInteractions(evidenceAgentService);
        assertEquals(Optional.empty(), service().getLast(12L));
    }

    @Test
    void a2aRequestIsCollectedWithTheCallersRequestId() {
        EvidenceAgentRequest request = new EvidenceAgentRequest(12L, 7L, "My holiday", "I went to Penang.", Language.ENGLISH);
        EvidenceCollectionResult result = result(12L);
        when(evidenceAgentService.collect(request, "req-1")).thenReturn(result);

        assertSame(result, service().collect(request, "req-1"));
        assertEquals(Optional.of(result), service().getLast(12L));
        verifyNoInteractions(compositionClient);
    }

    @Test
    void a2aRequestWithoutContentIsRejected() {
        EvidenceAgentRequest request = new EvidenceAgentRequest(12L, 7L, "Empty", null, Language.ENGLISH);

        assertThrows(EvidenceGenerationException.class, () -> service().collect(request, "req-1"));
        verify(evidenceAgentService, org.mockito.Mockito.never()).collect(eq(request), anyString());
    }

    @Test
    void nothingIsRememberedForAnUnknownComposition() {
        assertEquals(Optional.empty(), service().getLast(99L));
    }

    private static EvidenceCollectionResult result(Long compositionId) {
        return new EvidenceCollectionResult(compositionId, List.of("Read the essay"), List.of(), null);
    }
}
