package com.aimp.model;

import java.util.List;

/**
 * Describes a handwritten interface or abstract class to implement.
 *
 * @param packageName the contract package name
 * @param simpleName the contract simple name
 * @param qualifiedName the contract fully qualified name
 * @param kind the contract kind
 * @param visibility the contract visibility
 * @param sourceSnippet the full source file content for the contract compilation unit when available
 * @param typeParameters the contract type parameters
 * @param annotations annotations declared on the contract type
 * @param constructors constructors declared by the contract
 * @param methods annotated abstract methods to implement
 */
public record ContractModel(
    String packageName,
    String simpleName,
    String qualifiedName,
    ContractKind kind,
    Visibility visibility,
    String sourceSnippet,
    List<TypeParameterModel> typeParameters,
    List<AnnotationUsage> annotations,
    List<ConstructorModel> constructors,
    List<MethodModel> methods
) {
    /**
     * Creates an immutable contract model.
     *
     * @param packageName the contract package name
     * @param simpleName the contract simple name
     * @param qualifiedName the contract fully qualified name
     * @param kind the contract kind
     * @param visibility the contract visibility
     * @param sourceSnippet the full source file content for the contract compilation unit when available
     * @param typeParameters the contract type parameters
     * @param annotations annotations declared on the contract type
     * @param constructors constructors declared by the contract
     * @param methods annotated abstract methods to implement
     */
    public ContractModel {
        sourceSnippet = sourceSnippet == null ? "" : sourceSnippet;
        typeParameters = List.copyOf(typeParameters);
        annotations = List.copyOf(annotations);
        constructors = List.copyOf(constructors);
        methods = List.copyOf(methods);
    }
}
