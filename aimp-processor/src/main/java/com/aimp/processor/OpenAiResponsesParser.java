package com.aimp.processor;

final class OpenAiResponsesParser {
    private OpenAiResponsesParser() {
    }

    static String extractOutputText(String json) {
        String nestedOutputText = extractNestedOutputText(json);
        if (nestedOutputText != null) {
            return nestedOutputText;
        }

        String topLevelOutputText = extractTopLevelStringField(json, "output_text");
        if (topLevelOutputText != null) {
            return topLevelOutputText;
        }

        String refusal = extractNestedRefusal(json);
        if (refusal == null) {
            refusal = extractTopLevelStringField(json, "refusal");
        }
        if (refusal != null) {
            throw new MethodBodySynthesisException("OpenAI refused to synthesize a method body: " + refusal);
        }

        throw new MethodBodySynthesisException("OpenAI response did not contain output text.");
    }

    private static String extractNestedOutputText(String json) {
        int outputIndex = json.indexOf("\"output\"");
        if (outputIndex < 0) {
            return null;
        }

        int searchIndex = outputIndex;
        while (true) {
            int typeIndex = json.indexOf("\"type\"", searchIndex);
            if (typeIndex < 0) {
                return null;
            }

            String type = JsonStringFieldExtractor.extractOptionalStringAfter(json, "type", typeIndex);
            if ("output_text".equals(type)) {
                return JsonStringFieldExtractor.extractRequiredString(
                    json.substring(typeIndex),
                    "text"
                );
            }

            searchIndex = typeIndex + "\"type\"".length();
        }
    }

    private static String extractNestedRefusal(String json) {
        int outputIndex = json.indexOf("\"output\"");
        if (outputIndex < 0) {
            return null;
        }

        int searchIndex = outputIndex;
        while (true) {
            int typeIndex = json.indexOf("\"type\"", searchIndex);
            if (typeIndex < 0) {
                return null;
            }

            String type = JsonStringFieldExtractor.extractOptionalStringAfter(json, "type", typeIndex);
            if ("refusal".equals(type)) {
                return JsonStringFieldExtractor.extractRequiredString(
                    json.substring(typeIndex),
                    "refusal"
                );
            }

            searchIndex = typeIndex + "\"type\"".length();
        }
    }

    private static String extractTopLevelStringField(String json, String fieldName) {
        String marker = "\"" + fieldName + "\"";
        int markerIndex = json.indexOf(marker);
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
        if (valueStart >= json.length()) {
            throw new MethodBodySynthesisException("Synthesis response field '" + fieldName + "' is malformed.");
        }
        if (json.charAt(valueStart) != '"') {
            return null;
        }

        return JsonStringFieldExtractor.extractOptionalStringAfter(json, fieldName, 0);
    }
}
