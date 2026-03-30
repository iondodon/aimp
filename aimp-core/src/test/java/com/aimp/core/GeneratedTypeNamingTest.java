package com.aimp.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GeneratedTypeNamingTest {
    @Test
    void appendsGeneratedSuffix() {
        assertEquals("PaymentService_AIGenerated", GeneratedTypeNaming.generatedSimpleName("PaymentService"));
    }

    @Test
    void buildsQualifiedName() {
        assertEquals(
            "com.example.PaymentService_AIGenerated",
            GeneratedTypeNaming.generatedQualifiedName("com.example", "PaymentService")
        );
    }
}
