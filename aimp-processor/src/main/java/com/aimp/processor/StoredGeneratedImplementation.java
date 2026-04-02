package com.aimp.processor;

import java.util.List;

record StoredGeneratedImplementation(
    String contractQualifiedName,
    String contractFingerprintHash,
    String generatedQualifiedName,
    String generatedSource,
    List<StoredContextDependency> contextDependencies
) {
    StoredGeneratedImplementation {
        generatedSource = generatedSource == null ? "" : generatedSource;
        contextDependencies = List.copyOf(contextDependencies);
    }
}
