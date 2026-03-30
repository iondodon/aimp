package com.aimp.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OpenAiResponsesParserTest {
    @Test
    void extractsNestedOutputTextFromResponsesApiPayload() {
        String json = """
            {
              "output_text": [
                {
                  "type": "output_text",
                  "text": "ignored top-level array entry"
                }
              ],
              "output": [
                {
                  "type": "message",
                  "role": "assistant",
                  "content": [
                    {
                      "type": "output_text",
                      "text": "return 42;",
                      "annotations": []
                    }
                  ]
                }
              ]
            }
            """;

        assertEquals("return 42;", OpenAiResponsesParser.extractOutputText(json));
    }

    @Test
    void stillSupportsLegacyTopLevelOutputTextString() {
        String json = """
            {
              "output_text": "return 7;"
            }
            """;

        assertEquals("return 7;", OpenAiResponsesParser.extractOutputText(json));
    }

    @Test
    void surfacesRefusalsClearly() {
        String json = """
            {
              "output": [
                {
                  "type": "message",
                  "role": "assistant",
                  "content": [
                    {
                      "type": "refusal",
                      "refusal": "Unsafe request"
                    }
                  ]
                }
              ]
            }
            """;

        MethodBodySynthesisException exception = assertThrows(
            MethodBodySynthesisException.class,
            () -> OpenAiResponsesParser.extractOutputText(json)
        );

        assertEquals(
            "OpenAI refused to synthesize a method body: Unsafe request",
            exception.getMessage()
        );
    }
}
