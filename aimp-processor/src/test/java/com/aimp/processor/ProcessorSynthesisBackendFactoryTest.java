package com.aimp.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ProcessorSynthesisBackendFactoryTest {
    @Test
    void usesOpenAiApiKeyEnvVarByDefault() {
        AtomicReference<String> requestedEnvVar = new AtomicReference<>();
        ProcessorSynthesisBackendFactory factory = new ProcessorSynthesisBackendFactory(name -> {
            requestedEnvVar.set(name);
            return "env-openai-key";
        });

        Object synthesizer = factory.create(Map.of());

        assertEquals("OPENAI_API_KEY", requestedEnvVar.get());
        assertInstanceOf(OpenAiGeneratedClassSynthesizer.class, synthesizer);
    }

    @Test
    void failsClearlyWhenOpenAiApiKeyMissing() {
        ProcessorSynthesisBackendFactory factory = new ProcessorSynthesisBackendFactory(name -> null);

        MethodBodySynthesisException exception = assertThrows(
            MethodBodySynthesisException.class,
            () -> factory.create(Map.of())
        );

        assertTrue(exception.getMessage().contains("OPENAI_API_KEY"));
    }
}
