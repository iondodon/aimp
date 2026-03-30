package com.aimp.processor;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aimp.model.ContractKind;
import com.aimp.model.ContractModel;
import com.aimp.model.MethodModel;
import com.aimp.model.Visibility;
import java.util.List;
import org.junit.jupiter.api.Test;

class MethodBodySynthesisPromptFactoryTest {
    @Test
    void includesContractSourceSnippetWhenAvailable() {
        MethodModel method = new MethodModel(
            "charge",
            "com.example.payment.PaymentResult",
            Visibility.PUBLIC,
            List.of(),
            List.of(),
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

        String prompt = MethodBodySynthesisPromptFactory.prompt(contract, method);

        assertTrue(prompt.contains("Use the full contract source below as the primary context."));
        assertTrue(prompt.contains("Do not emit generic placeholders such as TODO, stub, or Not implemented."));
        assertTrue(prompt.contains("throw new java.lang.UnsupportedOperationException(\"AIMP could not synthesize a concrete implementation for com.example.payment.PaymentService#charge."));
        assertTrue(prompt.contains("public interface PaymentService {"));
        assertTrue(prompt.contains("PaymentResult charge(PaymentRequest request);"));
    }
}
