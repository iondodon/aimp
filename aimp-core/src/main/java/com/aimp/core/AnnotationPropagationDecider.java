package com.aimp.core;

import com.aimp.model.AnnotationUsage;
import com.aimp.model.GeneratedElementKind;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class AnnotationPropagationDecider {
    private static final String AI_IMPLEMENTED_ANNOTATION = "com.aimp.annotations.AIImplemented";

    public List<AnnotationUsage> propagate(
        List<AnnotationUsage> annotations,
        Set<String> allowlist,
        GeneratedElementKind targetKind
    ) {
        return annotations.stream()
            .filter(annotation -> allowlist.contains(annotation.typeName()))
            .filter(annotation -> !annotation.typeName().equals(AI_IMPLEMENTED_ANNOTATION))
            .filter(annotation -> annotation.applicableTargets().contains(targetKind))
            .sorted(Comparator.comparing(AnnotationUsage::typeName))
            .toList();
    }
}
