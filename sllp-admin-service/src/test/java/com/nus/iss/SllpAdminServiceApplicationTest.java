package com.nus.iss;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

class SllpAdminServiceApplicationTest {

    @Test
    void mainDelegatesToSpringApplicationWithoutStartingContext() {
        String[] args = {"--server.port=0"};

        try (MockedStatic<SpringApplication> spring = Mockito.mockStatic(SpringApplication.class)) {
            SllpAdminServiceApplication.main(args);

            spring.verify(() -> SpringApplication.run(SllpAdminServiceApplication.class, args));
        }
    }
}
