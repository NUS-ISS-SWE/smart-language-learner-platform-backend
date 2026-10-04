package com.nus.iss;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

class SllpEvaluationServiceApplicationTest {

    @Test
    void mainDelegatesWithoutStartingSpringContext() {
        String[] args = {"--server.port=0"};

        try (MockedStatic<SpringApplication> spring = Mockito.mockStatic(SpringApplication.class)) {
            SllpEvaluationServiceApplication.main(args);

            spring.verify(() -> SpringApplication.run(SllpEvaluationServiceApplication.class, args));
        }
    }
}
