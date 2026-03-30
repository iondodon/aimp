package com.aimp.processor;

final class JsonStringFieldExtractor {
    private JsonStringFieldExtractor() {
    }

    static String extractRequiredString(String json, String fieldName) {
        String value = extractOptionalString(json, fieldName);
        if (value == null) {
            throw new MethodBodySynthesisException("Synthesis response did not include the required field '" + fieldName + "'.");
        }
        return value;
    }

    static String extractOptionalString(String json, String fieldName) {
        return extractOptionalStringAfter(json, fieldName, 0);
    }

    static String extractOptionalStringAfter(String json, String fieldName, int startIndex) {
        String marker = "\"" + fieldName + "\"";
        int markerIndex = json.indexOf(marker, startIndex);
        if (markerIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(':', markerIndex + marker.length());
        if (colonIndex < 0) {
            throw new MethodBodySynthesisException("Synthesis response field '" + fieldName + "' is malformed.");
        }

        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        if (valueStart >= json.length() || json.charAt(valueStart) != '"') {
            throw new MethodBodySynthesisException("Synthesis response field '" + fieldName + "' must be a JSON string.");
        }

        StringBuilder builder = new StringBuilder();
        for (int index = valueStart + 1; index < json.length(); index++) {
            char current = json.charAt(index);
            if (current == '\\') {
                if (index + 1 >= json.length()) {
                    throw new MethodBodySynthesisException("Synthesis response contains an invalid escape sequence.");
                }
                char escaped = json.charAt(++index);
                switch (escaped) {
                    case '"', '\\', '/' -> builder.append(escaped);
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    case 'u' -> {
                        if (index + 4 >= json.length()) {
                            throw new MethodBodySynthesisException("Synthesis response contains an invalid unicode escape.");
                        }
                        String hex = json.substring(index + 1, index + 5);
                        builder.append((char) Integer.parseInt(hex, 16));
                        index += 4;
                    }
                    default -> throw new MethodBodySynthesisException("Unsupported JSON escape sequence '\\" + escaped + "'.");
                }
                continue;
            }
            if (current == '"') {
                return builder.toString();
            }
            builder.append(current);
        }

        throw new MethodBodySynthesisException("Synthesis response field '" + fieldName + "' was not terminated.");
    }

    static String escape(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (current < 0x20) {
                        builder.append(String.format("\\u%04x", (int) current));
                    } else {
                        builder.append(current);
                    }
                }
            }
        }
        return builder.toString();
    }
}
