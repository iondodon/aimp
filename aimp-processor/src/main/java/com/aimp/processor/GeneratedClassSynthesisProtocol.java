package com.aimp.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;

final class GeneratedClassSynthesisProtocol {
    static final String PROTOCOL_VERSION = "1";

    static final String RESPONSE_TYPE_GENERATED_CLASS = "generated_class";
    static final String RESPONSE_TYPE_REQUEST_CONTEXT_TYPES = "request_context_types";
    static final String RESPONSE_TYPE_INSUFFICIENT_CONTEXT = "insufficient_context";

    private GeneratedClassSynthesisProtocol() {
    }

    static String instructionsJson() {
        ObjectNode root = JsonSupport.objectNode();
        root.put("protocolVersion", PROTOCOL_VERSION);
        root.put("role", "compile_time_java_class_generator");
        ArrayNode outputRules = root.putArray("outputRules");
        outputRules.add("Return exactly one JSON object and nothing else.");
        outputRules.add("Do not return markdown or code fences.");
        outputRules.add("The JSON response must include protocolVersion and responseType.");
        outputRules.add("Use only the responseType values listed in input.responseContract.responseTypeValues for the current round.");

        ObjectNode responseContract = root.putObject("responseContract");
        ArrayNode responseTypeValues = responseContract.putArray("responseTypeValues");
        responseTypeValues.add(RESPONSE_TYPE_GENERATED_CLASS);
        responseTypeValues.add(RESPONSE_TYPE_REQUEST_CONTEXT_TYPES);
        responseTypeValues.add(RESPONSE_TYPE_INSUFFICIENT_CONTEXT);
        responseContract.put("generatedClassSource", "Required when responseType is generated_class.");
        responseContract.put(
            "requestedTypeNames",
            "Required when responseType is request_context_types. Use only concrete fully qualified Java type names."
        );
        responseContract.put("callerMessage", "Required when responseType is insufficient_context.");
        return JsonSupport.writeJson(root, "synthesis instructions");
    }

    static SynthesisResponse parseResponse(String rawResponse) {
        String normalized = normalizeRawResponse(rawResponse);
        JsonNode root = JsonSupport.readTree(normalized, "OpenAI synthesis response");
        if (!root.isObject()) {
            throw new MethodBodySynthesisException("OpenAI synthesis response must be a JSON object.");
        }

        String protocolVersion = JsonSupport.requireText(root, "protocolVersion", "OpenAI synthesis response");
        if (!PROTOCOL_VERSION.equals(protocolVersion)) {
            throw new MethodBodySynthesisException(
                "OpenAI synthesis response used unsupported protocolVersion '" + protocolVersion + "'."
            );
        }

        String responseType = JsonSupport.requireText(root, "responseType", "OpenAI synthesis response");
        return switch (responseType) {
            case RESPONSE_TYPE_GENERATED_CLASS -> new SynthesisResponse(
                responseType,
                JsonSupport.requireText(root, "generatedClassSource", "OpenAI synthesis response"),
                List.of(),
                ""
            );
            case RESPONSE_TYPE_REQUEST_CONTEXT_TYPES -> new SynthesisResponse(
                responseType,
                "",
                JsonSupport.requireTextArray(root, "requestedTypeNames", "OpenAI synthesis response"),
                ""
            );
            case RESPONSE_TYPE_INSUFFICIENT_CONTEXT -> new SynthesisResponse(
                responseType,
                "",
                List.of(),
                JsonSupport.requireText(root, "callerMessage", "OpenAI synthesis response")
            );
            default -> throw new MethodBodySynthesisException(
                "OpenAI synthesis response used unsupported responseType '" + responseType + "'."
            );
        };
    }

    private static String normalizeRawResponse(String rawResponse) {
        String trimmed = rawResponse == null ? "" : rawResponse.trim();
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

    record SynthesisResponse(
        String responseType,
        String generatedClassSource,
        List<String> requestedTypeNames,
        String callerMessage
    ) {
        SynthesisResponse {
            requestedTypeNames = List.copyOf(requestedTypeNames);
            generatedClassSource = generatedClassSource == null ? "" : generatedClassSource;
            callerMessage = callerMessage == null ? "" : callerMessage;
        }
    }
}
