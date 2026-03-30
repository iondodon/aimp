package com.aimp.runtime;

import java.util.List;
import java.util.Objects;

public record AimpInvocationRequest(
    String contractTypeName,
    String methodName,
    String description,
    String returnTypeName,
    List<AimpArgument> arguments
) {
    public AimpInvocationRequest {
        Objects.requireNonNull(contractTypeName, "contractTypeName");
        Objects.requireNonNull(methodName, "methodName");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(returnTypeName, "returnTypeName");
        arguments = List.copyOf(arguments);
    }
}
