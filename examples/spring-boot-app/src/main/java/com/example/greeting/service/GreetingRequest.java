package com.example.greeting.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request payload accepted by the Spring Boot greeting example.
 *
 * @param namee caller name used in the greeting
 * @param language requested language code such as {@code en}, {@code ro}, or {@code es}
 * @param excited whether the greeting should use stronger punctuation
 */
public record GreetingRequest(
    @NotBlank String namee,
    @NotBlank
    @Pattern(regexp = "en|ro|es", message = "language must be one of: en, ro, es")
    String language,
    boolean excited
) {
}
