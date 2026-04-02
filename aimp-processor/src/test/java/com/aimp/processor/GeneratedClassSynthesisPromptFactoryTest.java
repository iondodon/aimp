package com.aimp.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aimp.model.ContractKind;
import com.aimp.model.ContractModel;
import com.aimp.model.MethodModel;
import com.aimp.model.ParameterModel;
import com.aimp.model.ReferencedTypeModel;
import com.aimp.model.Visibility;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeneratedClassSynthesisPromptFactoryTest {
    @Test
    void includesContractSourceSnippetWhenAvailable() {
        MethodModel method = new MethodModel(
            "charge",
            "com.example.payment.PaymentResult",
            Visibility.PUBLIC,
            List.of(),
            List.of(new ParameterModel("request", "com.example.payment.PaymentRequest", false, List.of())),
            List.of(),
            List.of(),
            "Charge a payment"
        );
        ContractModel contract = new ContractModel(
            "com.example.payment",
            "PaymentService",
            "com.example.payment.PaymentService",
            "7",
            ContractKind.INTERFACE,
            Visibility.PUBLIC,
            """
            package com.example.payment;

            import com.aimp.annotations.AIImplemented;

            public interface PaymentService {
                @AIImplemented("Charge a payment")
                PaymentResult charge(PaymentRequest request);
            }
            """,
            List.of(),
            List.of(),
            List.of(),
            List.of(method)
        );

        String prompt = GeneratedClassSynthesisPromptFactory.prompt(
            contract,
            List.of(
                new ReferencedTypeModel(
                    "com.example.payment.PaymentRequest",
                    "public record PaymentRequest(String reference) {\n}",
                    2
                )
            ),
            new ContextRequestFeedback(
                List.of("com.example.payment.PaymentRequest"),
                List.of(new RejectedContextTypeRequest(
                    "com.example.payment.MissingType",
                    "AIMP could not supply this type because its source is unavailable or it is not reachable from the contract context graph."
                ))
            ),
            2,
            3
        );

        JsonNode root = JsonSupport.readTree(prompt, "generated class synthesis prompt");

        assertEquals("1", root.path("protocolVersion").asText());
        assertEquals("generate_generated_class", root.path("task").asText());
        assertEquals(2, root.path("round").path("current").asInt());
        assertEquals(3, root.path("round").path("max").asInt());
        assertEquals("PaymentService_AIGenerated", root.path("generationTarget").path("generatedSimpleName").asText());
        assertEquals("7", root.path("generationTarget").path("contractVersion").asText());
        assertEquals("implement", root.path("generationTarget").path("relationship").asText());
        assertEquals(
            List.of("generated_class", "request_context_types"),
            textArray(root.path("responseContract").path("responseTypeValues"))
        );
        assertTrue(root.path("responseContract").path("callerMessageRule").isMissingNode());
        assertTrue(textArray(root.path("constraints")).contains(
            "Do not duplicate annotations or mix declaration annotations with equivalent type-use annotations on the same element."
        ));
        assertTrue(textArray(root.path("constraints")).contains(
            "Do not use reflection, method-name scanning, Method.invoke, or other dynamic invocation unless the contract explicitly requires it."
        ));
        assertTrue(textArray(root.path("constraints")).contains(
            "If implementation depends on a collaborator API that is not present in contractSource or includedTypeContexts, request more context instead of guessing or using dynamic invocation."
        ));
        assertTrue(textArray(root.path("constraints")).contains(
            "When requesting more context, request only fully qualified Java type names. Do not request methods, fields, packages, wildcards, or prose descriptions."
        ));
        assertTrue(textArray(root.path("constraints")).contains(
            "AIMP validates requested types after each round and reports fulfilled and rejected requests in contextRequestFeedback."
        ));
        assertTrue(textArray(root.path("constraints")).contains(
            "Do not return responseType insufficient_context before the final round. In non-final rounds, return generated_class or request_context_types."
        ));
        assertTrue(root.path("contractSource").asText().contains("package com.example.payment;"));
        assertTrue(root.path("contractSource").asText().contains("import com.aimp.annotations.AIImplemented;"));
        assertTrue(root.path("contractSource").asText().contains("@AIImplemented(\"Charge a payment\")"));
        assertTrue(root.path("contractSource").asText().contains("PaymentResult charge(PaymentRequest request);"));
        assertEquals("com.example.payment.PaymentRequest", root.path("includedTypeContexts").get(0).path("qualifiedName").asText());
        assertTrue(root.path("includedTypeContexts").get(0).path("source").asText().contains("public record PaymentRequest(String reference) {"));
        assertTrue(root.path("includedTypeContexts").get(0).path("nextLayerTypeNames").isMissingNode());
        assertEquals(
            List.of("com.example.payment.PaymentRequest"),
            textArray(root.path("contextRequestFeedback").path("fulfilledTypeNames"))
        );
        assertEquals(
            "com.example.payment.MissingType",
            root.path("contextRequestFeedback").path("rejectedTypeRequests").get(0).path("qualifiedName").asText()
        );
        assertTrue(root.path("annotatedMethods").isMissingNode());
        assertTrue(root.path("accessibleConstructors").isMissingNode());
        assertTrue(root.path("availableNextLayerTypeNames").isMissingNode());
    }

    @Test
    void finalRoundAllowsInsufficientContext() {
        ContractModel contract = new ContractModel(
            "com.example.payment",
            "PaymentService",
            "com.example.payment.PaymentService",
            "1",
            ContractKind.INTERFACE,
            Visibility.PUBLIC,
            "package com.example.payment;\n\npublic interface PaymentService {}",
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );

        JsonNode root = JsonSupport.readTree(
            GeneratedClassSynthesisPromptFactory.prompt(contract, List.of(), null, 3, 3),
            "generated class synthesis prompt"
        );

        assertEquals(
            List.of("generated_class", "request_context_types", "insufficient_context"),
            textArray(root.path("responseContract").path("responseTypeValues"))
        );
        assertEquals(
            "Use when responseType is insufficient_context. Ask the caller for missing business rules, examples, or handwritten code.",
            root.path("responseContract").path("callerMessageRule").asText()
        );
        assertTrue(textArray(root.path("constraints")).contains(
            "Use responseType insufficient_context only in this final round when the current context still cannot support a safe implementation."
        ));
    }

    private static List<String> textArray(JsonNode node) {
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        node.forEach(item -> values.add(item.asText()));
        return List.copyOf(values);
    }
}
