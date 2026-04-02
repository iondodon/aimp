package com.aimp.model;

import java.util.List;

/**
 * Describes an abstract contract method that the generated implementation must provide.
 *
 * @param name the method name
 * @param returnType the method return type
 * @param visibility the method visibility
 * @param typeParameters the method type parameters
 * @param parameters the method parameters
 * @param thrownTypes the checked exceptions declared by the method
 * @param annotations annotations declared on the method
 */
public record MethodModel(
    String name,
    String returnType,
    Visibility visibility,
    List<TypeParameterModel> typeParameters,
    List<ParameterModel> parameters,
    List<String> thrownTypes,
    List<AnnotationUsage> annotations
) {
    /**
     * Creates an immutable method model.
     *
     * @param name the method name
     * @param returnType the method return type
     * @param visibility the method visibility
     * @param typeParameters the method type parameters
     * @param parameters the method parameters
     * @param thrownTypes the checked exceptions declared by the method
     * @param annotations annotations declared on the method
     */
    public MethodModel {
        typeParameters = List.copyOf(typeParameters);
        parameters = List.copyOf(parameters);
        thrownTypes = List.copyOf(thrownTypes);
        annotations = List.copyOf(annotations);
    }
}
