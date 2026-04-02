package com.aimp.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aimp.model.ConstructorModel;
import com.aimp.model.ContractKind;
import com.aimp.model.ContractModel;
import com.aimp.model.MethodModel;
import com.aimp.model.ParameterModel;
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
    void stripsAbstractAndFinalFromGeneratedClassDeclaration() {
        String sanitized = GeneratedClassSourceSanitizer.sanitize("""
            package com.example.payment;

            public abstract final class PaymentService_AIGenerated implements PaymentService {
                @Override
                public java.lang.String charge() {
                    return "ok";
                }
            }
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
    void addsMissingImportsFromContractSignatures() {
        ContractModel contract = new ContractModel(
            "com.example.greeting.api",
            "GreetingController",
            "com.example.greeting.api.GreetingController",
            "1",
            ContractKind.ABSTRACT_CLASS,
            Visibility.PUBLIC,
            "",
            List.of(),
            List.of(),
            List.of(new ConstructorModel(
                Visibility.PUBLIC,
                List.of(new ParameterModel("greetingService", "com.example.greeting.service.GreetingService", false, List.of())),
                List.of()
            )),
            List.of(new MethodModel(
                "greet",
                "com.example.greeting.service.GreetingResponse",
                Visibility.PUBLIC,
                List.of(),
                List.of(new ParameterModel("request", "com.example.greeting.service.GreetingRequest", false, List.of())),
                List.of(),
                List.of(),
                "Call the service"
            ))
        );

        String sanitized = GeneratedClassSourceSanitizer.sanitize("""
            package com.example.greeting.api;

            public class GreetingController_AIGenerated extends GreetingController {
                public GreetingController_AIGenerated(GreetingService greetingService) {
                    super(greetingService);
                }

                @Override
                public GreetingResponse greet(GreetingRequest request) {
                    return this.greetingService.greet(request);
                }
            }
            """, contract);

        assertEquals("""
            package com.example.greeting.api;

            import com.example.greeting.service.GreetingRequest;
            import com.example.greeting.service.GreetingResponse;
            import com.example.greeting.service.GreetingService;

            public class GreetingController_AIGenerated extends GreetingController {
                public GreetingController_AIGenerated(GreetingService greetingService) {
                    super(greetingService);
                }

                @Override
                public GreetingResponse greet(GreetingRequest request) {
                    return this.greetingService.greet(request);
                }
            }
            """.trim(), sanitized.trim());
    }

    private static ContractModel contract() {
        return new ContractModel(
            "com.example.payment",
            "PaymentService",
            "com.example.payment.PaymentService",
            "1",
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
