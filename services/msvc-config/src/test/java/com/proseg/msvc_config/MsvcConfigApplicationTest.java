package com.proseg.msvc_config;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class MsvcConfigApplicationTest {

    @Test
    void mainDelegatesStartupToSpringApplication() {
        String[] args = {"--spring.main.web-application-type=none"};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            MsvcConfigApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(MsvcConfigApplication.class, args));
        }
    }
}
