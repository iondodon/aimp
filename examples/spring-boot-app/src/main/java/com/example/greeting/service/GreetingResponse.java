package com.example.greeting.service;

/**
 * Response returned by the Spring Boot greeting example.
 *
 * @param message rendered greeting message
 * @param resolvedLanguage language code actually used to create the greeting
 */
public record GreetingResponse(String message, String resolvedLanguage) {
}
