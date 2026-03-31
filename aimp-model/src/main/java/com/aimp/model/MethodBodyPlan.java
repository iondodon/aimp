package com.aimp.model;

import java.util.List;

/**
 * Holds the Java statements for a generated method body.
 *
 * @param statements the method body statements without surrounding braces
 */
public record MethodBodyPlan(List<String> statements) {
    /**
     * Creates an immutable method body plan.
     *
     * @param statements the method body statements without surrounding braces
     */
    public MethodBodyPlan {
        statements = List.copyOf(statements);
    }
}
