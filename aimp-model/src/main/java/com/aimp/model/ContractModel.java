package com.aimp.model;

import java.util.List;

public record ContractModel(
    String packageName,
    String simpleName,
    String qualifiedName,
    ContractKind kind,
    Visibility visibility,
    String sourceSnippet,
    List<ReferencedTypeModel> referencedTypes,
    List<TypeParameterModel> typeParameters,
    List<AnnotationUsage> annotations,
    List<ConstructorModel> constructors,
    List<MethodModel> methods
) {
    public ContractModel {
        sourceSnippet = sourceSnippet == null ? "" : sourceSnippet;
        referencedTypes = List.copyOf(referencedTypes);
        typeParameters = List.copyOf(typeParameters);
        annotations = List.copyOf(annotations);
        constructors = List.copyOf(constructors);
        methods = List.copyOf(methods);
    }
}
