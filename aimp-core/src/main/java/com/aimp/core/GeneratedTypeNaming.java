package com.aimp.core;

/**
 * Naming helpers for generated implementation types.
 */
public final class GeneratedTypeNaming {
    /**
     * The suffix appended to handwritten contract names for generated implementations.
     */
    public static final String GENERATED_SUFFIX = "_AIGenerated";

    private GeneratedTypeNaming() {
    }

    /**
     * Returns the generated implementation simple name for a contract.
     *
     * @param contractSimpleName the handwritten contract simple name
     * @return the generated implementation simple name
     */
    public static String generatedSimpleName(String contractSimpleName) {
        return contractSimpleName + GENERATED_SUFFIX;
    }

    /**
     * Returns the generated implementation fully qualified name for a contract.
     *
     * @param packageName the contract package name
     * @param contractSimpleName the handwritten contract simple name
     * @return the generated implementation fully qualified name
     */
    public static String generatedQualifiedName(String packageName, String contractSimpleName) {
        String simpleName = generatedSimpleName(contractSimpleName);
        return packageName == null || packageName.isBlank() ? simpleName : packageName + "." + simpleName;
    }
}
