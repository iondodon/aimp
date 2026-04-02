package com.aimp.model;

/**
 * Describes extra type context that may be sent to the LLM in later rounds.
 *
 * @param qualifiedName the referenced type's fully qualified name
 * @param sourceSnippet the full source file content for the referenced type when available
 * @param layer the round where the type was added to LLM context
 */
public record ReferencedTypeModel(
    String qualifiedName,
    String sourceSnippet,
    int layer
) {
    /**
     * Creates an immutable referenced type model.
     *
     * @param qualifiedName the referenced type's fully qualified name
     * @param sourceSnippet the full source file content for the referenced type when available
     * @param layer the round where the type was added to LLM context
     */
    public ReferencedTypeModel {
        sourceSnippet = sourceSnippet == null ? "" : sourceSnippet;
        if (layer < 1) {
            throw new IllegalArgumentException("layer must be at least 1");
        }
    }
}
