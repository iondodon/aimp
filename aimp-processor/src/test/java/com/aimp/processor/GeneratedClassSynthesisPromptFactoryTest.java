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
            List.of(
                new ReferencedTypeModel(
                    "com.example.payment.PaymentRequest",
                    "public record PaymentRequest(String reference) {\n}",
                    1,
                    List.of("com.example.payment.PaymentResult")
                ),
                new ReferencedTypeModel(
                    "com.example.payment.PaymentResult",
                    "public record PaymentResult(String status) {\n}",
                    2,
                    List.of()
                )
            ),
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
                    1,
                    List.of("com.example.payment.PaymentResult")
                )
            ),
            List.of("com.example.payment.PaymentResult"),
            2,
            3
        );

        JsonNode root = JsonSupport.readTree(prompt, "generated class synthesis prompt");

        assertEquals("1", root.path("protocolVersion").asText());
        assertEquals("generate_generated_class", root.path("task").asText());
        assertEquals(2, root.path("round").path("current").asInt());
        assertEquals(3, root.path("round").path("max").asInt());
        assertEquals("PaymentService_AIGenerated", root.path("generationTarget").path("generatedSimpleName").asText());
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
            "Do not return responseType insufficient_context before the final round. In non-final rounds, return generated_class or request_context_types."
        ));
        assertEquals(List.of("com.example.payment.PaymentResult"), textArray(root.path("availableNextLayerTypeNames")));
        assertTrue(root.path("contractSource").asText().contains("package com.example.payment;"));
        assertTrue(root.path("contractSource").asText().contains("import com.aimp.annotations.AIImplemented;"));
        assertTrue(root.path("contractSource").asText().contains("@AIImplemented(\"Charge a payment\")"));
        assertTrue(root.path("contractSource").asText().contains("PaymentResult charge(PaymentRequest request);"));
        assertEquals("com.example.payment.PaymentRequest", root.path("includedTypeContexts").get(0).path("qualifiedName").asText());
        assertTrue(root.path("includedTypeContexts").get(0).path("source").asText().contains("public record PaymentRequest(String reference) {"));
        assertTrue(root.path("annotatedMethods").isMissingNode());
        assertTrue(root.path("accessibleConstructors").isMissingNode());
    }

    @Test
    void finalRoundAllowsInsufficientContext() {
        ContractModel contract = new ContractModel(
            "com.example.payment",
            "PaymentService",
            "com.example.payment.PaymentService",
            ContractKind.INTERFACE,
            Visibility.PUBLIC,
            "package com.example.payment;\n\npublic interface PaymentService {}",
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );

        JsonNode root = JsonSupport.readTree(
            GeneratedClassSynthesisPromptFactory.prompt(contract, List.of(), List.of(), 3, 3),
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
