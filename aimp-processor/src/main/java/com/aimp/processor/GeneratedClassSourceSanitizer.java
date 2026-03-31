package com.aimp.processor;

import com.aimp.core.GeneratedTypeNaming;
import com.aimp.model.AnnotationUsage;
import com.aimp.model.ContractKind;
import com.aimp.model.ContractModel;
import com.aimp.model.ConstructorModel;
import com.aimp.model.MethodModel;
import com.aimp.model.ParameterModel;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GeneratedClassSourceSanitizer {
    private static final Pattern IMPORT_PATTERN = Pattern.compile("(?m)^import\\s+([^;]+);\\s*$");
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("(?m)^package\\s+[^;]+;\\s*$");
    private static final Pattern QUALIFIED_TYPE_PATTERN = Pattern.compile("\\b(?:[a-z_]\\w*\\.)+[A-Z_$][\\w$]*\\b");

    private GeneratedClassSourceSanitizer() {
    }

    static String sanitize(String rawSource, ContractModel contract) {
        String sanitized = stripMarkdownFences(rawSource == null ? "" : rawSource).trim();
        sanitized = stripAiImplementedImport(sanitized);
        sanitized = stripAiImplementedAnnotations(sanitized).trim();
        sanitized = normalizeGeneratedClassModifiers(sanitized, contract);
        sanitized = mergeRequiredImports(sanitized, contract);
        sanitized = collapseBlankLines(sanitized);
        if (sanitized.isBlank()) {
            throw new MethodBodySynthesisException("OpenAI synthesis returned an empty generated class.");
        }

        validatePackage(contract, sanitized);
        validateClassName(contract, sanitized);
        validateInheritance(contract, sanitized);
        validateNoAiImplemented(sanitized);
        return sanitized + System.lineSeparator();
    }

    private static String normalizeGeneratedClassModifiers(String source, ContractModel contract) {
        String generatedSimpleName = Pattern.quote(GeneratedTypeNaming.generatedSimpleName(contract.simpleName()));
        String sanitized = source.replaceAll(
            "(?m)\\babstract\\s+(?=(?:final\\s+)?class\\s+" + generatedSimpleName + "\\b)",
            ""
        );
        sanitized = sanitized.replaceAll(
            "(?m)\\bfinal\\s+(?=(?:abstract\\s+)?class\\s+" + generatedSimpleName + "\\b)",
            ""
        );
        return sanitized;
    }

    private static String mergeRequiredImports(String source, ContractModel contract) {
        String normalized = source.replace("\r\n", "\n");
        TreeSet<String> imports = new TreeSet<>(extractExistingImports(normalized));
        imports.addAll(requiredImports(contract));
        if (imports.isEmpty()) {
            return normalized;
        }

        Matcher packageMatcher = PACKAGE_PATTERN.matcher(normalized);
        String packageDeclaration = "";
        int bodyStart = 0;
        if (packageMatcher.find()) {
            packageDeclaration = packageMatcher.group().trim();
            bodyStart = packageMatcher.end();
        }

        String body = normalized.substring(bodyStart);
        body = IMPORT_PATTERN.matcher(body).replaceAll("");
        body = body.replaceFirst("^\\s+", "");

        StringBuilder builder = new StringBuilder(normalized.length() + imports.size() * 32);
        if (!packageDeclaration.isEmpty()) {
            builder.append(packageDeclaration).append("\n\n");
        }
        for (String importType : imports) {
            builder.append("import ").append(importType).append(";\n");
        }
        if (!imports.isEmpty()) {
            builder.append('\n');
        }
        builder.append(body);
        return builder.toString().trim();
    }

    private static Set<String> extractExistingImports(String source) {
        TreeSet<String> imports = new TreeSet<>();
        Matcher matcher = IMPORT_PATTERN.matcher(source);
        while (matcher.find()) {
            imports.add(matcher.group(1).trim());
        }
        return imports;
    }

    private static Set<String> requiredImports(ContractModel contract) {
        LinkedHashSet<String> imports = new LinkedHashSet<>();
        contract.annotations().forEach(annotation -> collectImportType(imports, annotation.typeName(), contract));
        for (ConstructorModel constructor : contract.constructors()) {
            constructor.parameters().forEach(parameter -> collectImports(imports, parameter, contract));
            constructor.thrownTypes().forEach(type -> collectQualifiedTypes(imports, type, contract));
        }
        for (MethodModel method : contract.methods()) {
            collectQualifiedTypes(imports, method.returnType(), contract);
            method.annotations().forEach(annotation -> collectImportType(imports, annotation.typeName(), contract));
            method.parameters().forEach(parameter -> collectImports(imports, parameter, contract));
            method.thrownTypes().forEach(type -> collectQualifiedTypes(imports, type, contract));
        }
        return imports;
    }

    private static void collectImports(LinkedHashSet<String> imports, ParameterModel parameter, ContractModel contract) {
        collectQualifiedTypes(imports, parameter.type(), contract);
        for (AnnotationUsage annotation : parameter.annotations()) {
            collectImportType(imports, annotation.typeName(), contract);
        }
    }

    private static void collectImportType(LinkedHashSet<String> imports, String typeName, ContractModel contract) {
        if (typeName == null || typeName.isBlank()) {
            return;
        }
        addImportCandidate(imports, typeName, contract);
    }

    private static void collectQualifiedTypes(LinkedHashSet<String> imports, String renderedType, ContractModel contract) {
        if (renderedType == null || renderedType.isBlank()) {
            return;
        }
        Matcher matcher = QUALIFIED_TYPE_PATTERN.matcher(renderedType);
        while (matcher.find()) {
            addImportCandidate(imports, matcher.group(), contract);
        }
    }

    private static void addImportCandidate(LinkedHashSet<String> imports, String qualifiedTypeName, ContractModel contract) {
        int lastDot = qualifiedTypeName.lastIndexOf('.');
        if (lastDot < 0) {
            return;
        }
        String packageName = qualifiedTypeName.substring(0, lastDot);
        if (packageName.equals("java.lang") || packageName.equals(contract.packageName())) {
            return;
        }
        if (qualifiedTypeName.equals(contract.qualifiedName())) {
            return;
        }
        imports.add(qualifiedTypeName);
    }

    private static void validatePackage(ContractModel contract, String source) {
        String expectedPackage = "package " + contract.packageName() + ";";
        if (!source.contains(expectedPackage)) {
            throw new MethodBodySynthesisException(
                "OpenAI synthesis returned a generated class with the wrong package. Expected " + expectedPackage
            );
        }
    }

    private static void validateClassName(ContractModel contract, String source) {
        String generatedSimpleName = GeneratedTypeNaming.generatedSimpleName(contract.simpleName());
        Pattern pattern = Pattern.compile("\\bclass\\s+" + Pattern.quote(generatedSimpleName) + "\\b");
        if (!pattern.matcher(source).find()) {
            throw new MethodBodySynthesisException(
                "OpenAI synthesis returned a generated class with the wrong name. Expected " + generatedSimpleName
            );
        }
    }

    private static void validateInheritance(ContractModel contract, String source) {
        String relationship = contract.kind() == ContractKind.INTERFACE ? "implements" : "extends";
        Pattern simplePattern = Pattern.compile(
            "\\b" + relationship + "\\b[^\\{;]*\\b" + Pattern.quote(contract.simpleName()) + "\\b"
        );
        Pattern qualifiedPattern = Pattern.compile(
            "\\b" + relationship + "\\b[^\\{;]*\\b" + Pattern.quote(contract.qualifiedName()) + "\\b"
        );
        if (!simplePattern.matcher(source).find() && !qualifiedPattern.matcher(source).find()) {
            throw new MethodBodySynthesisException(
                "OpenAI synthesis returned a generated class that does not " + relationship + " " + contract.qualifiedName()
            );
        }
    }

    private static void validateNoAiImplemented(String source) {
        if (containsAiImplementedAnnotation(source)) {
            throw new MethodBodySynthesisException("OpenAI synthesis returned source that still contains @AIImplemented.");
        }
    }

    private static String stripAiImplementedImport(String source) {
        return source
            .replace("import com.aimp.annotations.AIImplemented;\r\n", "")
            .replace("import com.aimp.annotations.AIImplemented;\n", "");
    }

    private static String stripAiImplementedAnnotations(String source) {
        StringBuilder builder = new StringBuilder(source.length());
        int index = 0;
        while (index < source.length()) {
            if (source.startsWith("\"\"\"", index)) {
                int nextIndex = skipTextBlock(source, index + 3);
                builder.append(source, index, nextIndex);
                index = nextIndex;
                continue;
            }
            if (source.startsWith("//", index)) {
                int nextIndex = skipLineComment(source, index + 2);
                builder.append(source, index, nextIndex);
                index = nextIndex;
                continue;
            }
            if (source.startsWith("/*", index)) {
                int nextIndex = skipBlockComment(source, index + 2);
                builder.append(source, index, nextIndex);
                index = nextIndex;
                continue;
            }
            char current = source.charAt(index);
            if (current == '"') {
                int nextIndex = skipQuotedLiteral(source, index + 1, '"');
                builder.append(source, index, nextIndex);
                index = nextIndex;
                continue;
            }
            if (current == '\'') {
                int nextIndex = skipQuotedLiteral(source, index + 1, '\'');
                builder.append(source, index, nextIndex);
                index = nextIndex;
                continue;
            }
            if (source.startsWith("@AIImplemented", index)) {
                index = skipAnnotation(source, index + "@AIImplemented".length());
                continue;
            }
            if (source.startsWith("@com.aimp.annotations.AIImplemented", index)) {
                index = skipAnnotation(source, index + "@com.aimp.annotations.AIImplemented".length());
                continue;
            }
            builder.append(source.charAt(index));
            index++;
        }
        return builder.toString();
    }

    private static boolean containsAiImplementedAnnotation(String source) {
        int index = 0;
        while (index < source.length()) {
            if (source.startsWith("\"\"\"", index)) {
                index = skipTextBlock(source, index + 3);
                continue;
            }
            if (source.startsWith("//", index)) {
                index = skipLineComment(source, index + 2);
                continue;
            }
            if (source.startsWith("/*", index)) {
                index = skipBlockComment(source, index + 2);
                continue;
            }
            char current = source.charAt(index);
            if (current == '"') {
                index = skipQuotedLiteral(source, index + 1, '"');
                continue;
            }
            if (current == '\'') {
                index = skipQuotedLiteral(source, index + 1, '\'');
                continue;
            }
            if (source.startsWith("@AIImplemented", index) || source.startsWith("@com.aimp.annotations.AIImplemented", index)) {
                return true;
            }
            index++;
        }
        return false;
    }

    private static int skipAnnotation(String source, int index) {
        while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
            index++;
        }
        if (index < source.length() && source.charAt(index) == '(') {
            index = skipParenthesized(source, index);
        }
        while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
            index++;
        }
        return index;
    }

    private static int skipParenthesized(String source, int index) {
        int depth = 1;
        index++;
        while (index < source.length() && depth > 0) {
            if (source.startsWith("\"\"\"", index)) {
                index = skipTextBlock(source, index + 3);
                continue;
            }
            char current = source.charAt(index);
            if (current == '"') {
                index = skipQuotedLiteral(source, index + 1, '"');
                continue;
            }
            if (current == '\'') {
                index = skipQuotedLiteral(source, index + 1, '\'');
                continue;
            }
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
            }
            index++;
        }
        return index;
    }

    private static int skipTextBlock(String source, int index) {
        int closing = source.indexOf("\"\"\"", index);
        return closing < 0 ? source.length() : closing + 3;
    }

    private static int skipLineComment(String source, int index) {
        int lineBreak = source.indexOf('\n', index);
        return lineBreak < 0 ? source.length() : lineBreak;
    }

    private static int skipBlockComment(String source, int index) {
        int closing = source.indexOf("*/", index);
        return closing < 0 ? source.length() : closing + 2;
    }

    private static int skipQuotedLiteral(String source, int index, char quote) {
        while (index < source.length()) {
            char current = source.charAt(index);
            if (current == '\\') {
                index += 2;
                continue;
            }
            if (current == quote) {
                return index + 1;
            }
            index++;
        }
        return source.length();
    }

    private static String stripMarkdownFences(String source) {
        String trimmed = source.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstNewline = trimmed.indexOf('\n');
        if (firstNewline < 0) {
            return trimmed;
        }
        String body = trimmed.substring(firstNewline + 1);
        int closingFence = body.lastIndexOf("```");
        if (closingFence >= 0) {
            body = body.substring(0, closingFence);
        }
        return body.trim();
    }

    private static String collapseBlankLines(String source) {
        return source
            .replace("\r\n", "\n")
            .replaceAll("\n{3,}", "\n\n");
    }
}
