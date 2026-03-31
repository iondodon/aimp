package com.aimp.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class GeneratedClassSynthesisProtocolTest {
    @Test
    void parsesGeneratedClassResponse() {
        ObjectNode root = JsonSupport.objectNode();
        root.put("protocolVersion", "1");
        root.put("responseType", "generated_class");
        root.put("generatedClassSource", "package com.example;\nclass Example {}");
        GeneratedClassSynthesisProtocol.SynthesisResponse response =
            GeneratedClassSynthesisProtocol.parseResponse(JsonSupport.writeJson(root, "generated class protocol response"));

        assertEquals(GeneratedClassSynthesisProtocol.RESPONSE_TYPE_GENERATED_CLASS, response.responseType());
        assertEquals("package com.example;\nclass Example {}", response.generatedClassSource());
    }

    @Test
    void parsesRequestedContextTypeResponse() {
        ObjectNode root = JsonSupport.objectNode();
        root.put("protocolVersion", "1");
        root.put("responseType", "request_context_types");
        ArrayNode requestedTypeNames = root.putArray("requestedTypeNames");
        requestedTypeNames.add("com.example.Foo");
        requestedTypeNames.add("com.example.Bar");
        GeneratedClassSynthesisProtocol.SynthesisResponse response =
            GeneratedClassSynthesisProtocol.parseResponse(JsonSupport.writeJson(root, "request context protocol response"));

        assertEquals(
            java.util.List.of("com.example.Foo", "com.example.Bar"),
            response.requestedTypeNames()
        );
    }

    @Test
    void parsesInsufficientContextResponse() {
        ObjectNode root = JsonSupport.objectNode();
        root.put("protocolVersion", "1");
        root.put("responseType", "insufficient_context");
        root.put("callerMessage", "Add business rules.");
        GeneratedClassSynthesisProtocol.SynthesisResponse response =
            GeneratedClassSynthesisProtocol.parseResponse(JsonSupport.writeJson(root, "insufficient context protocol response"));

        assertEquals(GeneratedClassSynthesisProtocol.RESPONSE_TYPE_INSUFFICIENT_CONTEXT, response.responseType());
        assertEquals("Add business rules.", response.callerMessage());
    }

    @Test
    void rejectsNonJsonResponses() {
        MethodBodySynthesisException exception = assertThrows(
            MethodBodySynthesisException.class,
            () -> GeneratedClassSynthesisProtocol.parseResponse("not json")
        );

        assertEquals("Failed to parse OpenAI synthesis response as JSON.", exception.getMessage());
    }
}
