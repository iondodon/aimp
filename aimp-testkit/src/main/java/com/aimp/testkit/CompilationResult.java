package com.aimp.testkit;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

public record CompilationResult(
    boolean success,
    List<Diagnostic<? extends JavaFileObject>> diagnostics,
    Path generatedSourcesDirectory,
    Path classesDirectory
) {
    public List<String> messages(Diagnostic.Kind kind) {
        return diagnostics.stream()
            .filter(diagnostic -> diagnostic.getKind() == kind)
            .map(diagnostic -> diagnostic.getMessage(java.util.Locale.ROOT))
            .collect(Collectors.toList());
    }

    public String generatedSource(String relativePath) throws IOException {
        return Files.readString(generatedSourcesDirectory.resolve(relativePath), StandardCharsets.UTF_8);
    }

    public boolean hasGeneratedSource(String relativePath) {
        return Files.exists(generatedSourcesDirectory.resolve(relativePath));
    }
}
