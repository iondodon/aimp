package com.aimp.model;

import java.util.List;

public record MethodModel(
    String name,
    String returnType,
    Visibility visibility,
    List<TypeParameterModel> typeParameters,
    List<ParameterModel> parameters,
    List<String> thrownTypes,
    List<AnnotationUsage> annotations,
    String description
) {
    public MethodModel {
        typeParameters = List.copyOf(typeParameters);
        parameters = List.copyOf(parameters);
        thrownTypes = List.copyOf(thrownTypes);
        annotations = List.copyOf(annotations);
    }
}
