package com.aimp.runtime;

public final class AimpInvocationException extends RuntimeException {
    public AimpInvocationException(String message) {
        super(message);
    }

    public AimpInvocationException(String message, Throwable cause) {
        super(message, cause);
    }
}
