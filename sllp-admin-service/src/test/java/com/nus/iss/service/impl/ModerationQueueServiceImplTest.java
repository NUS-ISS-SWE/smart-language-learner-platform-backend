package com.nus.iss.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nus.iss.dto.ModerationQueueResponse;
import com.nus.iss.repository.ModerationQueueRepository;

@ExtendWith(MockitoExtension.class)
class ModerationQueueServiceImplTest {
    @Mock private ModerationQueueRepository repository;

    @Test
    void normalizesFiltersAndUsesExclusiveEndDate() {
        ModerationQueueServiceImpl service = new ModerationQueueServiceImpl(repository);
        ModerationQueueResponse expected = new ModerationQueueResponse(List.of(), 0, 0, 20, false);
        when(repository.find("REMOVED", 7L, "FILE_PDF", "ENGLISH",
                LocalDate.of(2026, 9, 1).atStartOfDay(), LocalDate.of(2026, 9, 4).atStartOfDay(), 0, 20))
                .thenReturn(expected);

        assertEquals(expected, service.list(" removed ", 7L, "file_pdf", "english",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3), 0, 20));
        verify(repository).find("REMOVED", 7L, "FILE_PDF", "ENGLISH",
                LocalDate.of(2026, 9, 1).atStartOfDay(), LocalDate.of(2026, 9, 4).atStartOfDay(), 0, 20);
    }

    @Test
    void rejectsInvalidPaginationAndDateRange() {
        ModerationQueueServiceImpl service = new ModerationQueueServiceImpl(repository);
        assertThrows(IllegalArgumentException.class,
                () -> service.list(null, null, null, null, null, null, -1, 20));
        assertThrows(IllegalArgumentException.class,
                () -> service.list(null, null, null, null, LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 1), 0, 20));
    }
}
