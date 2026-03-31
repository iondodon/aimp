package com.aimp.model;

import java.util.List;

/**
 * Describes a generated method implementation.
 *
 * @param name the generated method name
 * @param returnType the generated method return type
 * @param visibility the generated method visibility
 * @param typeParameters the generated method type parameters
 * @param parameters the generated method parameters
 * @param thrownTypes the checked exceptions declared by the generated method
 * @param annotations annotations to render on the generated method
 * @param bodyPlan the generated method body plan
 */
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
    /**
     * Creates an immutable generated method plan.
     *
     * @param name the generated method name
     * @param returnType the generated method return type
     * @param visibility the generated method visibility
     * @param typeParameters the generated method type parameters
     * @param parameters the generated method parameters
     * @param thrownTypes the checked exceptions declared by the generated method
     * @param annotations annotations to render on the generated method
     * @param bodyPlan the generated method body plan
     */
    public GeneratedMethodPlan {
        typeParameters = List.copyOf(typeParameters);
        parameters = List.copyOf(parameters);
        thrownTypes = List.copyOf(thrownTypes);
        annotations = List.copyOf(annotations);
    }
}
