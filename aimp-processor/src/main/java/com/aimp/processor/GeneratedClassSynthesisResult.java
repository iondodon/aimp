package com.aimp.processor;

import com.aimp.model.ReferencedTypeModel;
import java.util.List;

record GeneratedClassSynthesisResult(
    String generatedSource,
    List<ReferencedTypeModel> usedReferencedTypes
) {
    GeneratedClassSynthesisResult {
        generatedSource = generatedSource == null ? "" : generatedSource;
        usedReferencedTypes = List.copyOf(usedReferencedTypes);
    }
}
