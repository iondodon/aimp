package com.aimp.model;

public record ReferencedTypeModel(String qualifiedName, String sourceSnippet) {
    public ReferencedTypeModel {
        sourceSnippet = sourceSnippet == null ? "" : sourceSnippet;
    }
}
