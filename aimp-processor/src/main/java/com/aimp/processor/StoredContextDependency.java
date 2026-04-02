package com.aimp.processor;

record StoredContextDependency(
    String qualifiedName,
    String fingerprintHash
) {
    StoredContextDependency {
        if (qualifiedName == null || qualifiedName.isBlank()) {
            throw new IllegalArgumentException("qualifiedName must not be blank");
        }
        if (fingerprintHash == null || fingerprintHash.isBlank()) {
            throw new IllegalArgumentException("fingerprintHash must not be blank");
        }
    }
}
