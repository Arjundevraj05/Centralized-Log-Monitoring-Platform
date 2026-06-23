package com.logmonitor;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test verifying the Spring application context loads.
 */
@SpringBootTest
@ActiveProfiles("test")
@Disabled("Requires running PostgreSQL instance with Flyway migrations applied")
class LogMonitorApplicationTests {

    @Test
    void contextLoads() {
        // Context load verification; requires PostgreSQL for full integration startup.
    }
}
