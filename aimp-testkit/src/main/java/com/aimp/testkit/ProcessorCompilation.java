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

/**
 * Compiles Java sources with an in-process annotation processor for tests.
 */
public final class ProcessorCompilation {
    private ProcessorCompilation() {
    }

    /**
     * Compiles the supplied source files with additional project files and compiler options.
     *
     * @param projectDirectory temporary project root used for compilation inputs and outputs
     * @param processor annotation processor under test
     * @param sourceFiles Java source files to compile
     * @param projectFiles extra non-source files to materialize in the project directory
     * @param additionalOptions extra javac options
     * @return compilation result with diagnostics and generated-output locations
     * @throws IOException if files cannot be written or outputs cannot be prepared
     */
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

        deleteDirectoryIfExists(projectDirectory.resolve("src"));
        deleteDirectoryIfExists(projectDirectory.resolve("classes"));
        deleteDirectoryIfExists(projectDirectory.resolve("generated"));

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
            options.add("-Aaimp.emitDiagnosticNotes=true");
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

    /**
     * Compiles only the supplied Java source files with the processor under test.
     *
     * @param projectDirectory temporary project root used for compilation inputs and outputs
     * @param processor annotation processor under test
     * @param sourceFiles Java source files to compile
     * @return compilation result with diagnostics and generated-output locations
     * @throws IOException if files cannot be written or outputs cannot be prepared
     */
    public static CompilationResult compile(
        Path projectDirectory,
        Processor processor,
        List<SourceFile> sourceFiles
    ) throws IOException {
        return compile(projectDirectory, processor, sourceFiles, Map.of(), List.of());
    }

    private static void deleteDirectoryIfExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            });
        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        }
    }
}
