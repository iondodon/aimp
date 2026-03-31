package com.aimp.model;

import java.util.List;

/**
 * Describes a handwritten constructor that a generated subtype must mirror.
 *
 * @param visibility the constructor visibility
 * @param parameters the constructor parameters
 * @param thrownTypes the checked exceptions declared by the constructor
 */
public record ConstructorModel(Visibility visibility, List<ParameterModel> parameters, List<String> thrownTypes) {
    /**
     * Creates an immutable constructor model.
     *
     * @param visibility the constructor visibility
     * @param parameters the constructor parameters
     * @param thrownTypes the checked exceptions declared by the constructor
     */
    public ConstructorModel {
        parameters = List.copyOf(parameters);
        thrownTypes = List.copyOf(thrownTypes);
    }
}
