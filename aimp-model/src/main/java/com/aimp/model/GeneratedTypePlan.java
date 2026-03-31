package com.aimp.model;

import java.util.List;

/**
 * Describes a generated implementation type.
 *
 * @param packageName the generated type package name
 * @param simpleName the generated type simple name
 * @param qualifiedName the generated type fully qualified name
 * @param contractSimpleName the handwritten contract simple name
 * @param contractKind the handwritten contract kind
 * @param visibility the generated type visibility
 * @param typeParameters the generated type parameters
 * @param annotations annotations to render on the generated type
 * @param constructors constructors to render on the generated type
 * @param methods methods to render on the generated type
 */
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
    /**
     * Creates an immutable generated type plan.
     *
     * @param packageName the generated type package name
     * @param simpleName the generated type simple name
     * @param qualifiedName the generated type fully qualified name
     * @param contractSimpleName the handwritten contract simple name
     * @param contractKind the handwritten contract kind
     * @param visibility the generated type visibility
     * @param typeParameters the generated type parameters
     * @param annotations annotations to render on the generated type
     * @param constructors constructors to render on the generated type
     * @param methods methods to render on the generated type
     */
    public GeneratedTypePlan {
        typeParameters = List.copyOf(typeParameters);
        annotations = List.copyOf(annotations);
        constructors = List.copyOf(constructors);
        methods = List.copyOf(methods);
    }
}
