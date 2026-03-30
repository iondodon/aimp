package com.aimp.runtime;

@FunctionalInterface
public interface AimpLlmExecutor {
    Object invoke(AimpInvocationRequest request);
}
