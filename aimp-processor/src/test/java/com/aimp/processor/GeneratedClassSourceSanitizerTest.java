package com.aimp.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aimp.model.ContractKind;
import com.aimp.model.ContractModel;
import com.aimp.model.Visibility;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeneratedClassSourceSanitizerTest {
    @Test
    void stripsMarkdownFencesAndValidatesSource() {
        String sanitized = GeneratedClassSourceSanitizer.sanitize("""
            ```java
            package com.example.payment;

            public class PaymentService_AIGenerated implements PaymentService {
                @Override
                public java.lang.String charge() {
                    return "ok";
                }
            }
            ```
            """, contract());

        assertEquals("""
            package com.example.payment;

            public class PaymentService_AIGenerated implements PaymentService {
                @Override
                public java.lang.String charge() {
                    return "ok";
                }
            }
            """.trim(), sanitized.trim());
    }

    @Test
    void stripsAiImplementedAnnotationsFromGeneratedSource() {
        String sanitized = GeneratedClassSourceSanitizer.sanitize(
            ("package com.example.payment;%n%n"
                + "import com.aimp.annotations.AIImplemented;%n%n"
                + "public class PaymentService_AIGenerated implements PaymentService {%n"
                + "    @AIImplemented(\"\"\"%n"
                + "        bad%n"
                + "        \"\"\")%n"
                + "    public java.lang.String charge() {%n"
                + "        return \"ok\";%n"
                + "    }%n"
                + "}%n").formatted(),
            contract()
        );

        assertEquals("""
            package com.example.payment;

            public class PaymentService_AIGenerated implements PaymentService {
                public java.lang.String charge() {
                    return "ok";
                }
            }
            """.trim(), sanitized.trim());
    }

    @Test
    void leavesStringLiteralsContainingAiImplementedAlone() {
        String sanitized = GeneratedClassSourceSanitizer.sanitize("""
            package com.example.payment;

            public class PaymentService_AIGenerated implements PaymentService {
                public java.lang.String charge() {
                    return "@AIImplemented";
                }
            }
            """, contract());

        assertEquals("""
            package com.example.payment;

            public class PaymentService_AIGenerated implements PaymentService {
                public java.lang.String charge() {
                    return "@AIImplemented";
                }
            }
            """.trim(), sanitized.trim());
    }

    @Test
    void failsWhenModelReportsInsufficientContext() {
        MethodBodySynthesisException exception = assertThrows(
            MethodBodySynthesisException.class,
            () -> GeneratedClassSourceSanitizer.sanitize(
                GeneratedClassSourceSanitizer.INSUFFICIENT_CONTEXT_SENTINEL,
                contract()
            )
        );

        assertEquals(
            "OpenAI reported insufficient contract context for com.example.payment.PaymentService. Add more context to @AIImplemented(\"...\") or the contract code.",
            exception.getMessage()
        );
    }

    @Test
    void failsWhenModelReturnsOldUnsupportedOperationFallback() {
        MethodBodySynthesisException exception = assertThrows(
            MethodBodySynthesisException.class,
            () -> GeneratedClassSourceSanitizer.sanitize("""
                package com.example.payment;

                public class PaymentService_AIGenerated implements PaymentService {
                    public java.lang.String charge() {
                        throw new java.lang.UnsupportedOperationException("AIMP could not synthesize a concrete implementation for com.example.payment.PaymentService#charge. Add more context to @AIImplemented(\\"...\\") or the contract code.");
                    }
                }
                """, contract())
        );

        assertEquals(
            "OpenAI reported insufficient contract context for com.example.payment.PaymentService. Add more context to @AIImplemented(\"...\") or the contract code.",
            exception.getMessage()
        );
    }

    private static ContractModel contract() {
        return new ContractModel(
            "com.example.payment",
            "PaymentService",
            "com.example.payment.PaymentService",
            ContractKind.INTERFACE,
            Visibility.PUBLIC,
            "",
            List.of(),
            List.of(),
            List.of(),
            List.of()
        );
    }
}
