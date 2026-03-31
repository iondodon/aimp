package com.aimp.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class OpenAiResponsesParserTest {
    @Test
    void extractsNestedOutputTextFromResponsesApiPayload() {
        ObjectNode root = JsonSupport.objectNode();
        ArrayNode topLevelOutputText = root.putArray("output_text");
        topLevelOutputText.addObject()
            .put("type", "output_text")
            .put("text", "ignored top-level array entry");
        ArrayNode output = root.putArray("output");
        ObjectNode message = output.addObject();
        message.put("type", "message");
        message.put("role", "assistant");
        ArrayNode content = message.putArray("content");
        ObjectNode outputText = content.addObject();
        outputText.put("type", "output_text");
        outputText.put("text", "return 42;");
        outputText.putArray("annotations");
        String json = JsonSupport.writeJson(root, "OpenAI responses payload");

        assertEquals("return 42;", OpenAiResponsesParser.extractOutputText(json));
    }

    @Test
    void stillSupportsLegacyTopLevelOutputTextString() {
        ObjectNode root = JsonSupport.objectNode();
        root.put("output_text", "return 7;");
        String json = JsonSupport.writeJson(root, "legacy OpenAI output payload");

        assertEquals("return 7;", OpenAiResponsesParser.extractOutputText(json));
    }

    @Test
    void surfacesRefusalsClearly() {
        ObjectNode root = JsonSupport.objectNode();
        ArrayNode output = root.putArray("output");
        ObjectNode message = output.addObject();
        message.put("type", "message");
        message.put("role", "assistant");
        ArrayNode content = message.putArray("content");
        content.addObject()
            .put("type", "refusal")
            .put("refusal", "Unsafe request");
        String json = JsonSupport.writeJson(root, "OpenAI refusal payload");

        MethodBodySynthesisException exception = assertThrows(
            MethodBodySynthesisException.class,
            () -> OpenAiResponsesParser.extractOutputText(json)
        );

        assertEquals(
            "OpenAI refused to synthesize a generated class: Unsafe request",
            exception.getMessage()
        );
    }
}
