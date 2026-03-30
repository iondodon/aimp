package com.aimp.model;

import java.util.List;

public record MethodBodyPlan(List<String> statements) {
    public MethodBodyPlan {
        statements = List.copyOf(statements);
    }
}
