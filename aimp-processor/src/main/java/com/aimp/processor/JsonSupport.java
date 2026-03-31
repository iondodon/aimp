package com.aimp.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;

final class JsonSupport {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonSupport() {
    }

    static ObjectNode objectNode() {
        return OBJECT_MAPPER.createObjectNode();
    }

    static ArrayNode arrayNode() {
        return OBJECT_MAPPER.createArrayNode();
    }

    static JsonNode readTree(String json, String description) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (JsonProcessingException exception) {
            throw new MethodBodySynthesisException("Failed to parse " + description + " as JSON.", exception);
        }
    }

    static String writeJson(JsonNode json, String description) {
        try {
            return OBJECT_MAPPER.writeValueAsString(json);
        } catch (JsonProcessingException exception) {
            throw new MethodBodySynthesisException("Failed to serialize " + description + " as JSON.", exception);
        }
    }

    static String requireText(JsonNode node, String fieldName, String description) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull() || !field.isTextual()) {
            throw new MethodBodySynthesisException(description + " did not include the required string field '" + fieldName + "'.");
        }
        return field.asText();
    }

    static List<String> requireTextArray(JsonNode node, String fieldName, String description) {
        JsonNode field = node.get(fieldName);
        if (field == null || field.isNull() || !field.isArray()) {
            throw new MethodBodySynthesisException(description + " did not include the required array field '" + fieldName + "'.");
        }
        ArrayList<String> values = new ArrayList<>();
        for (JsonNode element : field) {
            if (!element.isTextual()) {
                throw new MethodBodySynthesisException(description + " field '" + fieldName + "' must contain only strings.");
            }
            values.add(element.asText());
        }
        return List.copyOf(values);
    }
}
