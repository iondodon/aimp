package com.aimp.testkit;

public record SourceFile(String relativePath, String content) {
    public static SourceFile of(String relativePath, String content) {
        return new SourceFile(relativePath, content);
    }
}
