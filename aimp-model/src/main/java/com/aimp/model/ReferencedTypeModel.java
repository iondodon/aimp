package com.aimp.model;

import java.util.List;

/**
 * Describes extra type context that may be sent to the LLM in later rounds.
 *
 * @param qualifiedName the referenced type's fully qualified name
 * @param sourceSnippet the full source file content for the referenced type when available
 * @param layer the context expansion layer where the type becomes available
 * @param nextLayerTypeNames the reachable type names that may be requested next
 */
public record ReferencedTypeModel(
    String qualifiedName,
    String sourceSnippet,
    int layer,
    List<String> nextLayerTypeNames
) {
    /**
     * Creates an immutable referenced type model.
     *
     * @param qualifiedName the referenced type's fully qualified name
     * @param sourceSnippet the full source file content for the referenced type when available
     * @param layer the context expansion layer where the type becomes available
     * @param nextLayerTypeNames the reachable type names that may be requested next
     */
    public ReferencedTypeModel {
        sourceSnippet = sourceSnippet == null ? "" : sourceSnippet;
        if (layer < 1) {
            throw new IllegalArgumentException("layer must be at least 1");
        }
        nextLayerTypeNames = List.copyOf(nextLayerTypeNames);
    }
}
