package com.aimp.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.SourceVersion;

public final class AimpConfigLoader {
    public static final String CONFIG_FILE_NAME = "aimp.yml";

    public AimpConfig load(Path projectDir, ClassLoader classLoader) throws AimpConfigException {
        if (projectDir != null) {
            Path configPath = projectDir.resolve(CONFIG_FILE_NAME);
            if (Files.exists(configPath)) {
                return parse(readFile(configPath), configPath.toString());
            }
        }

        if (classLoader != null) {
            try (InputStream inputStream = classLoader.getResourceAsStream(CONFIG_FILE_NAME)) {
                if (inputStream != null) {
                    return parse(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8), "classpath:" + CONFIG_FILE_NAME);
                }
            } catch (IOException exception) {
                throw new AimpConfigException("Failed to read classpath config " + CONFIG_FILE_NAME, exception);
            }
        }

        return AimpConfig.EMPTY;
    }

    public AimpConfig parse(String content) throws AimpConfigException {
        return parse(content, "<memory>");
    }

    public AimpConfig parse(String content, String sourceDescription) throws AimpConfigException {
        List<String> annotations = new ArrayList<>();
        boolean inAimp = false;
        int aimpIndent = -1;
        boolean inPropagation = false;
        int propagationIndent = -1;
        boolean inAnnotations = false;
        int annotationsIndent = -1;

        String[] lines = content.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        for (int index = 0; index < lines.length; index++) {
            String rawLine = lines[index];
            String line = stripComment(rawLine);
            if (line.isBlank()) {
                continue;
            }

            int indent = indentation(line);
            String trimmed = line.trim();

            if (!inAimp) {
                if (indent == 0 && trimmed.equals("aimp:")) {
                    inAimp = true;
                    aimpIndent = indent;
                    continue;
                }
                continue;
            }

            if (indent <= aimpIndent) {
                inAimp = false;
                inPropagation = false;
                inAnnotations = false;
                if (indent == 0 && trimmed.equals("aimp:")) {
                    inAimp = true;
                    aimpIndent = indent;
                    continue;
                }
                continue;
            }

            if (inAnnotations && indent > annotationsIndent && trimmed.startsWith("- ")) {
                String value = unquote(trimmed.substring(2).trim());
                if (value.isBlank()) {
                    throw error(sourceDescription, index, "Annotation allowlist entry must not be empty");
                }
                if (!SourceVersion.isName(value)) {
                    throw error(sourceDescription, index, "Invalid annotation class name: " + value);
                }
                annotations.add(value);
                continue;
            }

            if (inAnnotations && indent <= annotationsIndent) {
                inAnnotations = false;
            }

            if (inPropagation && indent <= propagationIndent) {
                inPropagation = false;
            }

            if (trimmed.equals("propagation:")) {
                if (!inAimp) {
                    throw error(sourceDescription, index, "propagation must be nested under aimp");
                }
                inPropagation = true;
                propagationIndent = indent;
                continue;
            }

            if (trimmed.equals("annotations:")) {
                if (!inPropagation) {
                    throw error(sourceDescription, index, "annotations must be nested under aimp.propagation");
                }
                inAnnotations = true;
                annotationsIndent = indent;
                continue;
            }

            if (trimmed.startsWith("- ")) {
                throw error(sourceDescription, index, "List entries are only supported under aimp.propagation.annotations");
            }
        }

        return new AimpConfig(new PropagationConfig(annotations));
    }

    private static String readFile(Path configPath) throws AimpConfigException {
        try {
            return Files.readString(configPath, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new AimpConfigException("Failed to read config file " + configPath, exception);
        }
    }

    private static String stripComment(String line) {
        int hashIndex = line.indexOf('#');
        return hashIndex >= 0 ? line.substring(0, hashIndex) : line;
    }

    private static int indentation(String line) {
        int indent = 0;
        while (indent < line.length() && Character.isWhitespace(line.charAt(indent))) {
            indent++;
        }
        return indent;
    }

    private static String unquote(String value) {
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static AimpConfigException error(String sourceDescription, int lineIndex, String message) {
        return new AimpConfigException(sourceDescription + ":" + (lineIndex + 1) + ": " + message);
    }
}
