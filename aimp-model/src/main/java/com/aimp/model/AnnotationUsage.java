package com.aimp.model;

import java.util.Set;

public record AnnotationUsage(String typeName, String renderedSource, Set<GeneratedElementKind> applicableTargets) {
}
