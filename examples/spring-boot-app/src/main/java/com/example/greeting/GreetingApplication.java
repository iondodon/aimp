package com.example.greeting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Real Spring Boot example application that consumes an AIMP-generated service bean.
 */
@SpringBootApplication
public final class GreetingApplication {
    /**
     * Utility class.
     */
    private GreetingApplication() {
    }

    /**
     * Starts the Spring Boot example application.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(GreetingApplication.class, args);
    }
}
