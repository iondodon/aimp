package com.aimp.model;

import java.util.List;

public record GeneratedTypePlan(
    String packageName,
    String simpleName,
    String qualifiedName,
    String contractSimpleName,
    ContractKind contractKind,
    Visibility visibility,
    List<TypeParameterModel> typeParameters,
    List<AnnotationUsage> annotations,
    List<GeneratedConstructorPlan> constructors,
    List<GeneratedMethodPlan> methods
) {
    public GeneratedTypePlan {
        typeParameters = List.copyOf(typeParameters);
        annotations = List.copyOf(annotations);
        constructors = List.copyOf(constructors);
        methods = List.copyOf(methods);
    }
}
