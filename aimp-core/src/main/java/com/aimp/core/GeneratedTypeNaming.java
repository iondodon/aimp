package com.aimp.core;

public final class GeneratedTypeNaming {
    public static final String GENERATED_SUFFIX = "_AIGenerated";

    private GeneratedTypeNaming() {
    }

    public static String generatedSimpleName(String contractSimpleName) {
        return contractSimpleName + GENERATED_SUFFIX;
    }

    public static String generatedQualifiedName(String packageName, String contractSimpleName) {
        String simpleName = generatedSimpleName(contractSimpleName);
        return packageName == null || packageName.isBlank() ? simpleName : packageName + "." + simpleName;
    }
}
