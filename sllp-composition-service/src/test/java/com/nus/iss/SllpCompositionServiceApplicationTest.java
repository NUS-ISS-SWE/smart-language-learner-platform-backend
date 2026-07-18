package com.nus.iss;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

class SllpCompositionServiceApplicationTest {

    @Test
    void mainDelegatesToSpringApplicationRun() {
        String[] args = {"--server.port=0"};

        try (MockedStatic<SpringApplication> springApplication = Mockito.mockStatic(SpringApplication.class)) {
            SllpCompositionServiceApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(SllpCompositionServiceApplication.class, args));
        }
    }
}
