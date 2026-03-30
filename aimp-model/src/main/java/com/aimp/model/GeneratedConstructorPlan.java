package com.aimp.model;

import java.util.List;

public record GeneratedConstructorPlan(Visibility visibility, List<ParameterModel> parameters, List<String> thrownTypes) {
    public GeneratedConstructorPlan {
        parameters = List.copyOf(parameters);
        thrownTypes = List.copyOf(thrownTypes);
    }
}
