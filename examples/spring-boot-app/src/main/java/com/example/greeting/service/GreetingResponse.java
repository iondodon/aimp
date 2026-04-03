package com.example.greeting.service;

/**
 * Response returned by the Spring Boot greeting example.
 *
 * @param message rendered greeting message
 * @param resolvedLanguage language code actually used to create the greeting
 * @param appliedTone tone used to render the greeting
 * @param occasion occasion reflected in the greeting
 * @param vip whether the greeting used VIP wording
 */
public record GreetingResponse(
    String message,
    String resolvedLanguage,
    String appliedTone,
    String occasion,
    boolean vip
) {
}
