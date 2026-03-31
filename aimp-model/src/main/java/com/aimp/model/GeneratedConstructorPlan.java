package com.aimp.model;

import java.util.List;

/**
 * Describes a constructor to emit on the generated implementation type.
 *
 * @param visibility the generated constructor visibility
 * @param parameters the generated constructor parameters
 * @param thrownTypes the checked exceptions declared by the generated constructor
 */
public record GeneratedConstructorPlan(Visibility visibility, List<ParameterModel> parameters, List<String> thrownTypes) {
    /**
     * Creates an immutable generated constructor plan.
     *
     * @param visibility the generated constructor visibility
     * @param parameters the generated constructor parameters
     * @param thrownTypes the checked exceptions declared by the generated constructor
     */
    public GeneratedConstructorPlan {
        parameters = List.copyOf(parameters);
        thrownTypes = List.copyOf(thrownTypes);
    }
}
