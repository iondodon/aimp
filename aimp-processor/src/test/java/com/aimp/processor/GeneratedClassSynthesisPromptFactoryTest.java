package com.aimp.processor;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aimp.model.ContractKind;
import com.aimp.model.ContractModel;
import com.aimp.model.MethodModel;
import com.aimp.model.Visibility;
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

        String prompt = GeneratedClassSynthesisPromptFactory.prompt(contract);

        assertTrue(prompt.contains("Generate the complete Java source file for the generated implementation class."));
        assertTrue(prompt.contains("Generated class name: PaymentService_AIGenerated"));
        assertTrue(prompt.contains("If the handwritten contract contains framework or validation annotations"));
        assertTrue(prompt.contains("Do not duplicate annotations."));
        assertTrue(prompt.contains("AIMP_INSUFFICIENT_CONTEXT"));
        assertTrue(prompt.contains("Do not generate a fallback Java class in that case."));
        assertTrue(prompt.contains("public interface PaymentService {"));
        assertTrue(prompt.contains("PaymentResult charge(PaymentRequest request);"));
    }
}
