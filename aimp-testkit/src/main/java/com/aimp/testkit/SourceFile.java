package com.aimp.testkit;

/**
 * Describes one Java source file used by the compile-test harness.
 *
 * @param relativePath file path relative to the temporary source root
 * @param content file contents
 */
public record SourceFile(String relativePath, String content) {
    /**
     * Creates a source file descriptor.
     *
     * @param relativePath file path relative to the temporary source root
     * @param content file contents
     * @return source file descriptor
     */
    public static SourceFile of(String relativePath, String content) {
        return new SourceFile(relativePath, content);
    }
}
