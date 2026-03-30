package com.aimp.model;

import java.util.List;

public record ConstructorModel(Visibility visibility, List<ParameterModel> parameters, List<String> thrownTypes) {
    public ConstructorModel {
        parameters = List.copyOf(parameters);
        thrownTypes = List.copyOf(thrownTypes);
    }
}
