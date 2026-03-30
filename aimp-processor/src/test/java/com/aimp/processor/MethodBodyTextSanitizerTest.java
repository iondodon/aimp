package com.aimp.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MethodBodyTextSanitizerTest {
    @Test
    void appendsSemicolonToReturnStatementWhenModelOmitsIt() {
        assertEquals(
            "return value;",
            MethodBodyTextSanitizer.sanitize("return value")
        );
    }

    @Test
    void appendsSemicolonToThrowStatementWhenModelOmitsIt() {
        assertEquals(
            "throw new IllegalStateException(\"boom\");",
            MethodBodyTextSanitizer.sanitize("throw new IllegalStateException(\"boom\")")
        );
    }
}
