package com.nus.iss.repository;

import com.nus.iss.enums.GradingAttemptStatus;
import com.nus.iss.model.GradingAttempt;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class GradingRepositoryTest {
    @Autowired private GradingRepository repository;

    @Test
    void retrievesLatestAttemptForIdempotentRequestHandling() {
        repository.save(attempt(1, GradingAttemptStatus.FAILED));
        repository.save(attempt(2, GradingAttemptStatus.IN_PROGRESS));

        GradingAttempt latest = repository.findFirstByRequestIdOrderByAttemptNumberDesc("request-1").orElseThrow();
        assertEquals(2, latest.getAttemptNumber());
        assertEquals(GradingAttemptStatus.IN_PROGRESS, latest.getStatus());
    }

    private static GradingAttempt attempt(int number, GradingAttemptStatus status) {
        return GradingAttempt.builder().id(UUID.randomUUID()).requestId("request-1").contextId("workflow-1")
                .requestHash("a".repeat(64)).attemptNumber(number).status(status).userId(7L)
                .taskId(UUID.randomUUID().toString()).artifactId(UUID.randomUUID().toString())
                .receivedAt(LocalDateTime.now()).build();
    }
}
