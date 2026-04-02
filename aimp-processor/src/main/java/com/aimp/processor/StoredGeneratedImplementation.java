package com.aimp.processor;

record StoredGeneratedImplementation(
    String contractQualifiedName,
    String contractVersion,
    String generatedQualifiedName,
    String generatedSource
) {
    StoredGeneratedImplementation {
        generatedSource = generatedSource == null ? "" : generatedSource;
    }
}
