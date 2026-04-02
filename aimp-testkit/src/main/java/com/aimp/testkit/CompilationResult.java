package com.aimp.testkit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Captures the result of compiling source files with the AIMP test harness.
 *
 * @param success whether compilation succeeded
 * @param diagnostics compiler diagnostics produced during compilation
 * @param generatedSourcesDirectory directory containing generated sources
 * @param classesDirectory directory containing compiled classes
 */
public record CompilationResult(
    boolean success,
    List<Diagnostic<? extends JavaFileObject>> diagnostics,
    Path generatedSourcesDirectory,
    Path classesDirectory
) {
    /**
     * Returns all diagnostic messages for the requested compiler diagnostic kind.
     *
     * @param kind diagnostic kind to filter by
     * @return matching diagnostic messages
     */
    public List<String> messages(Diagnostic.Kind kind) {
        return diagnostics.stream()
            .filter(diagnostic -> diagnostic.getKind() == kind)
            .map(diagnostic -> diagnostic.getMessage(java.util.Locale.ROOT))
            .collect(Collectors.toList());
    }

    /**
     * Reads one generated source file from the generated sources directory.
     *
     * @param relativePath path relative to the generated sources directory
     * @return generated file contents
     * @throws IOException if the file cannot be read
     */
    public String generatedSource(String relativePath) throws IOException {
        return Files.readString(generatedSourcesDirectory.resolve(relativePath), StandardCharsets.UTF_8);
    }

    /**
     * Returns whether a generated source file exists.
     *
     * @param relativePath path relative to the generated sources directory
     * @return {@code true} when the generated file exists
     */
    public boolean hasGeneratedSource(String relativePath) {
        return Files.exists(generatedSourcesDirectory.resolve(relativePath));
    }
}
