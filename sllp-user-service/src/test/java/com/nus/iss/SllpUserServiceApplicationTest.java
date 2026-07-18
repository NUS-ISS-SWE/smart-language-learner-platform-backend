package com.nus.iss;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

class SllpUserServiceApplicationTest {

    @Test
    void mainDelegatesToSpringApplicationRun() {
        String[] args = {"--server.port=0"};

        try (MockedStatic<SpringApplication> springApplication = Mockito.mockStatic(SpringApplication.class)) {
            SllpUserServiceApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(SllpUserServiceApplication.class, args));
        }
    }
}
