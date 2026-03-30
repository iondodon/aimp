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
