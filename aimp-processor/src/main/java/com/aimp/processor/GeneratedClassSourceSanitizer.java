package com.aimp.processor;

import com.aimp.core.GeneratedTypeNaming;
import com.aimp.model.ContractKind;
import com.aimp.model.ContractModel;
import java.util.regex.Pattern;

final class GeneratedClassSourceSanitizer {
    private GeneratedClassSourceSanitizer() {
    }

    static String sanitize(String rawSource, ContractModel contract) {
        String sanitized = stripMarkdownFences(rawSource == null ? "" : rawSource).trim();
        sanitized = stripAiImplementedImport(sanitized);
        sanitized = stripAiImplementedAnnotations(sanitized).trim();
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
