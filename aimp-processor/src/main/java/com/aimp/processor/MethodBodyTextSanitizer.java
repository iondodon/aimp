package com.aimp.processor;

final class MethodBodyTextSanitizer {
    private MethodBodyTextSanitizer() {
    }

    static String sanitize(String body) {
        String trimmed = body.replace("\r\n", "\n").replace('\r', '\n').trim();

        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int closingFence = trimmed.lastIndexOf("```");
            if (firstNewline >= 0 && closingFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, closingFence).trim();
            }
        }

        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }

        return ensureTerminalSemicolon(trimmed);
    }

    private static String ensureTerminalSemicolon(String body) {
        if (body.isEmpty()) {
            return body;
        }

        int lastLineStart = body.lastIndexOf('\n');
        String lastLine = lastLineStart >= 0 ? body.substring(lastLineStart + 1).trim() : body;
        if (lastLine.isEmpty() || lastLine.endsWith(";") || lastLine.endsWith("{") || lastLine.endsWith("}")) {
            return body;
        }

        if (lastLine.startsWith("return ") || lastLine.startsWith("throw ")) {
            return body + ";";
        }

        return body;
    }
}
