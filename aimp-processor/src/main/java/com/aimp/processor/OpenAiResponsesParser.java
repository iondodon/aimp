package com.aimp.processor;

import com.fasterxml.jackson.databind.JsonNode;

final class OpenAiResponsesParser {
    private OpenAiResponsesParser() {
    }

    static String extractOutputText(String json) {
        JsonNode root = JsonSupport.readTree(json, "OpenAI response");
        String nestedOutputText = extractNestedOutputText(root.get("output"));
        if (nestedOutputText != null) {
            return nestedOutputText;
        }

        String topLevelOutputText = extractOutputTextNode(root.get("output_text"));
        if (topLevelOutputText != null) {
            return topLevelOutputText;
        }

        String refusal = extractNestedRefusal(root.get("output"));
        if (refusal == null) {
            JsonNode topLevelRefusal = root.get("refusal");
            if (topLevelRefusal != null && topLevelRefusal.isTextual()) {
                refusal = topLevelRefusal.asText();
            }
        }
        if (refusal != null) {
            throw new MethodBodySynthesisException("OpenAI refused to synthesize a generated class: " + refusal);
        }

        throw new MethodBodySynthesisException("OpenAI response did not contain output text.");
    }

    private static String extractNestedOutputText(JsonNode output) {
        if (output == null || !output.isArray()) {
            return null;
        }

        for (JsonNode outputItem : output) {
            JsonNode content = outputItem.get("content");
            if (content == null || !content.isArray()) {
                continue;
            }
            for (JsonNode contentItem : content) {
                if ("output_text".equals(contentItem.path("type").asText(null))) {
                    JsonNode text = contentItem.get("text");
                    if (text != null && text.isTextual()) {
                        return text.asText();
                    }
                }
            }
        }
        return null;
    }

    private static String extractNestedRefusal(JsonNode output) {
        if (output == null || !output.isArray()) {
            return null;
        }

        for (JsonNode outputItem : output) {
            JsonNode content = outputItem.get("content");
            if (content == null || !content.isArray()) {
                continue;
            }
            for (JsonNode contentItem : content) {
                if ("refusal".equals(contentItem.path("type").asText(null))) {
                    JsonNode refusal = contentItem.get("refusal");
                    if (refusal != null && refusal.isTextual()) {
                        return refusal.asText();
                    }
                }
            }
        }
        return null;
    }

    private static String extractOutputTextNode(JsonNode outputText) {
        if (outputText == null || outputText.isNull()) {
            return null;
        }
        if (outputText.isTextual()) {
            return outputText.asText();
        }
        if (outputText.isArray()) {
            for (JsonNode item : outputText) {
                if (item.isTextual()) {
                    return item.asText();
                }
                if (item.path("type").asText("").equals("output_text")) {
                    JsonNode text = item.get("text");
                    if (text != null && text.isTextual()) {
                        return text.asText();
                    }
                }
            }
        }
        return null;
    }
}
