package com.aimp.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aimp.model.AnnotationUsage;
import com.aimp.model.GeneratedElementKind;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AnnotationPropagationDeciderTest {
    private final AnnotationPropagationDecider decider = new AnnotationPropagationDecider();

    @Test
    void copiesOnlyAllowlistedSupportedAnnotations() {
        List<AnnotationUsage> annotations = List.of(
            new AnnotationUsage("com.example.Allowed", "@com.example.Allowed", Set.of(GeneratedElementKind.METHOD)),
            new AnnotationUsage("com.example.Other", "@com.example.Other", Set.of(GeneratedElementKind.METHOD)),
            new AnnotationUsage("com.example.ParamOnly", "@com.example.ParamOnly", Set.of(GeneratedElementKind.PARAMETER))
        );

        List<AnnotationUsage> propagated = decider.propagate(
            annotations,
            Set.of("com.example.Allowed", "com.example.ParamOnly"),
            GeneratedElementKind.METHOD
        );

        assertEquals(List.of("com.example.Allowed"), propagated.stream().map(AnnotationUsage::typeName).toList());
    }
}
