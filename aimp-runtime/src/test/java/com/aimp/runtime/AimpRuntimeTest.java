package com.aimp.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AimpRuntimeTest {
    @AfterEach
    void tearDown() {
        AimpRuntime.clearExecutor();
    }

    @Test
    void invokeUsesRegisteredExecutor() {
        AimpRuntime.setExecutor(request -> "ok:" + request.arguments().getFirst().value());

        Object value = AimpRuntime.invoke(
            new AimpInvocationRequest(
                "com.example.EchoService",
                "answer",
                "Answer the prompt",
                "java.lang.String",
                List.of(new AimpArgument("prompt", "java.lang.String", "hello"))
            )
        );

        assertEquals("ok:hello", value);
    }

    @Test
    void invokeFailsClearlyWithoutExecutor() {
        AimpRuntime.clearExecutor();

        AimpInvocationException exception = assertThrows(
            AimpInvocationException.class,
            () -> AimpRuntime.invoke(
                new AimpInvocationRequest(
                    "com.example.EchoService",
                    "answer",
                    "Answer the prompt",
                    "java.lang.String",
                    List.of()
                )
            )
        );

        assertTrue(exception.getMessage().contains("No AIMP LLM executor is configured"));
    }
}
