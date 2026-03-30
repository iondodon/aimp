package com.aimp.runtime;

import java.util.Iterator;
import java.util.Objects;
import java.util.ServiceLoader;

public final class AimpRuntime {
    private static volatile AimpLlmExecutor executor = loadExecutorFromServiceLoader();

    private AimpRuntime() {
    }

    public static void setExecutor(AimpLlmExecutor executor) {
        AimpRuntime.executor = Objects.requireNonNull(executor, "executor");
    }

    public static void clearExecutor() {
        executor = loadExecutorFromServiceLoader();
    }

    public static Object invoke(AimpInvocationRequest request) {
        Objects.requireNonNull(request, "request");

        AimpLlmExecutor currentExecutor = executor;
        if (currentExecutor == null) {
            throw new AimpInvocationException(
                "No AIMP LLM executor is configured. Register one with AimpRuntime.setExecutor(...) or ServiceLoader."
            );
        }

        try {
            return currentExecutor.invoke(request);
        } catch (AimpInvocationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AimpInvocationException(
                "AIMP LLM invocation failed for " + request.contractTypeName() + "#" + request.methodName(),
                exception
            );
        }
    }

    private static AimpLlmExecutor loadExecutorFromServiceLoader() {
        Iterator<AimpLlmExecutor> iterator = ServiceLoader.load(AimpLlmExecutor.class).iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }
}
