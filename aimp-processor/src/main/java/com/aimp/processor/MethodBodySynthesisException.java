package com.aimp.processor;

final class MethodBodySynthesisException extends RuntimeException {
    MethodBodySynthesisException(String message) {
        super(message);
    }

    MethodBodySynthesisException(String message, Throwable cause) {
        super(message, cause);
    }
}
