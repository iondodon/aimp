package com.aimp.model;

import java.util.Set;

/**
 * Describes an annotation that may be copied onto generated source.
 *
 * @param typeName the annotation's fully qualified type name
 * @param renderedSource the annotation as Java source
 * @param applicableTargets the generated element kinds the annotation may target
 */
public record AnnotationUsage(String typeName, String renderedSource, Set<GeneratedElementKind> applicableTargets) {
}
