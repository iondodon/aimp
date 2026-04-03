package com.example.greeting.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request payload accepted by the Spring Boot greeting example.
 *
 * @param name caller name used in the greeting
 * @param language requested language code such as {@code en}, {@code ro}, or {@code es}
 * @param occasion situation the greeting is for
 * @param tone preferred greeting tone
 * @param vip whether the caller should be acknowledged as a valued guest
 * @param excited whether the greeting should use stronger punctuation
 * @param includeSignature whether the response should end with a short sign-off
 * @param senderName optional signature name to use when a sign-off is included
 */
public record GreetingRequest(
    @NotBlank String name,
    @NotBlank
    @Pattern(regexp = "en|ro|es", message = "language must be one of: en, ro, es")
    String language,
    @NotBlank
    @Pattern(regexp = "welcome|birthday|farewell", message = "occasion must be one of: welcome, birthday, farewell")
    String occasion,
    @NotBlank
    @Pattern(regexp = "formal|friendly", message = "tone must be one of: formal, friendly")
    String tone,
    boolean vip,
    boolean excited,
    boolean includeSignature,
    String senderName
) {
}
