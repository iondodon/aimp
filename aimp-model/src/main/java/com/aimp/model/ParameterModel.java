package com.aimp.model;

import java.util.List;

/**
 * Describes a method or constructor parameter.
 *
 * @param name the parameter name
 * @param type the rendered parameter type
 * @param varArgs whether the parameter is declared as varargs
 * @param annotations annotations declared on the parameter
 */
public record ParameterModel(String name, String type, boolean varArgs, List<AnnotationUsage> annotations) {
    /**
     * Creates an immutable parameter model.
     *
     * @param name the parameter name
     * @param type the rendered parameter type
     * @param varArgs whether the parameter is declared as varargs
     * @param annotations annotations declared on the parameter
     */
    public ParameterModel {
        annotations = List.copyOf(annotations);
    }
}
