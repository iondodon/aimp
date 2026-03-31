package com.aimp.model;

/**
 * Describes a rendered type parameter.
 *
 * @param name the type parameter name
 * @param declaration the full declaration text, including bounds when present
 */
public record TypeParameterModel(String name, String declaration) {
}
