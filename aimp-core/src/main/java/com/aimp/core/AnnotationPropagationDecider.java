package com.aimp.core;

import com.aimp.model.AnnotationUsage;
import com.aimp.model.GeneratedElementKind;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Filters contract annotations down to the subset that may be copied to generated source.
 */
public final class AnnotationPropagationDecider {
    private static final String AI_IMPLEMENTED_ANNOTATION = "com.aimp.annotations.AIImplemented";

    /**
     * Creates a stateless annotation propagation decider.
     */
    public AnnotationPropagationDecider() {
    }

    /**
     * Returns the annotations that should be propagated to the given generated element kind.
     *
     * @param annotations the candidate annotations from handwritten source
     * @param allowlist the allowed annotation type names
     * @param targetKind the generated element kind receiving propagated annotations
     * @return the propagated annotations in deterministic order
     */
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
