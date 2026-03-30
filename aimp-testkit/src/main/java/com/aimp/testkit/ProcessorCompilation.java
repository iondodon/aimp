package com.aimp.testkit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.processing.Processor;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public final class ProcessorCompilation {
    private ProcessorCompilation() {
    }

    public static CompilationResult compile(
        Path projectDirectory,
        Processor processor,
        List<SourceFile> sourceFiles,
        Map<String, String> projectFiles,
        List<String> additionalOptions
    ) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No system Java compiler is available.");
        }

        Path sourceDirectory = Files.createDirectories(projectDirectory.resolve("src"));
        Path classesDirectory = Files.createDirectories(projectDirectory.resolve("classes"));
        Path generatedSourcesDirectory = Files.createDirectories(projectDirectory.resolve("generated"));

        for (SourceFile sourceFile : sourceFiles) {
            Path path = sourceDirectory.resolve(sourceFile.relativePath());
            Files.createDirectories(path.getParent());
            Files.writeString(path, sourceFile.content(), StandardCharsets.UTF_8);
        }
        for (Map.Entry<String, String> entry : projectFiles.entrySet()) {
            Path path = projectDirectory.resolve(entry.getKey());
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, entry.getValue(), StandardCharsets.UTF_8);
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            List<java.io.File> compilationUnits = sourceFiles.stream()
                .map(sourceFile -> sourceDirectory.resolve(sourceFile.relativePath()).toFile())
                .toList();

            List<String> options = new ArrayList<>();
            options.add("--release");
            options.add("21");
            options.add("-classpath");
            options.add(System.getProperty("java.class.path"));
            options.add("-d");
            options.add(classesDirectory.toString());
            options.add("-s");
            options.add(generatedSourcesDirectory.toString());
            options.add("-Aaimp.project.dir=" + projectDirectory);
            options.addAll(additionalOptions);

            JavaCompiler.CompilationTask task = compiler.getTask(
                null,
                fileManager,
                diagnostics,
                options,
                null,
                fileManager.getJavaFileObjectsFromFiles(compilationUnits)
            );
            task.setProcessors(List.of(processor));
            boolean success = Boolean.TRUE.equals(task.call());
            return new CompilationResult(success, List.copyOf(diagnostics.getDiagnostics()), generatedSourcesDirectory, classesDirectory);
        }
    }

    public static CompilationResult compile(
        Path projectDirectory,
        Processor processor,
        List<SourceFile> sourceFiles
    ) throws IOException {
        return compile(projectDirectory, processor, sourceFiles, Map.of(), List.of());
    }
}
