package com.logmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Centralized Log Monitoring Platform.
 *
 * <p>Provides secure log fetching, searching, and real-time streaming
 * from remote Linux/Tomcat servers via SSH.</p>
 */
@SpringBootApplication
public class LogMonitorApplication {

    /**
     * Bootstraps the Spring Boot application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(LogMonitorApplication.class, args);
    }
}
