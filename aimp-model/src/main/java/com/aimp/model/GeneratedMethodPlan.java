package com.aimp.model;

import java.util.List;

public record GeneratedMethodPlan(
    String name,
    String returnType,
    Visibility visibility,
    List<TypeParameterModel> typeParameters,
    List<ParameterModel> parameters,
    List<String> thrownTypes,
    List<AnnotationUsage> annotations,
    MethodBodyPlan bodyPlan
) {
    public GeneratedMethodPlan {
        typeParameters = List.copyOf(typeParameters);
        parameters = List.copyOf(parameters);
        thrownTypes = List.copyOf(thrownTypes);
        annotations = List.copyOf(annotations);
    }
}
