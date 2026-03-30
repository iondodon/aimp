package com.aimp.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aimp.testkit.CompilationResult;
import com.aimp.testkit.ProcessorCompilation;
import com.aimp.testkit.SourceFile;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.tools.Diagnostic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AimpProcessorCompileTest {
    @TempDir
    Path tempDir;

    @Test
    void generatesImplementationForInterface() throws Exception {
        CompilationResult result;
        try (FakeOpenAiServer server = FakeOpenAiServer.start(request -> {
            assertEquals("Bearer test-openai-key", request.authorization());
            assertTrue(request.body().contains("\"model\":\"gpt-5\""));
            assertTrue(request.body().contains("PaymentService"));
            assertTrue(request.body().contains("charge"));
            assertTrue(request.body().contains("Charge a payment and return the result"));
            return openAiOutputText("return new com.example.payment.PaymentResult(\"approved\");");
        })) {
            result = ProcessorCompilation.compile(
                tempDir.resolve("interface-project"),
                new AimpProcessor(),
                List.of(
                    SourceFile.of("com/example/payment/PaymentRequest.java", """
                        package com.example.payment;

                        public record PaymentRequest(String reference) {
                        }
                        """),
                    SourceFile.of("com/example/payment/PaymentResult.java", """
                        package com.example.payment;

                        public record PaymentResult(String status) {
                        }
                        """),
                    SourceFile.of("com/example/payment/PaymentService.java", """
                        package com.example.payment;

                        import com.aimp.annotations.AIImplemented;

                        public interface PaymentService {
                            @AIImplemented("Charge a payment and return the result")
                            PaymentResult charge(PaymentRequest request);
                        }
                        """)
                ),
                Map.of(),
                server.processorOptions()
            );
        }

        assertTrue(result.success(), () -> String.join("\n", result.messages(javax.tools.Diagnostic.Kind.ERROR)));
        assertTrue(result.hasGeneratedSource("com/example/payment/PaymentService_AIGenerated.java"));

        String expected = readResource("golden/PaymentService_AIGenerated.java");
        String actual = result.generatedSource("com/example/payment/PaymentService_AIGenerated.java");
        assertEquals(normalize(expected), normalize(actual));
    }

    @Test
    void openAiProviderGeneratesImplementation() throws Exception {
        CompilationResult result;
        try (FakeOpenAiServer server = FakeOpenAiServer.start(request -> {
            assertEquals("Bearer test-openai-key", request.authorization());
            assertTrue(request.body().contains("\"model\":\"gpt-5\""));
            assertTrue(request.body().contains("\"instructions\":\"You synthesize Java method bodies"));
            assertTrue(request.body().contains("Do not emit generic placeholders such as TODO, stub, or Not implemented."));
            assertTrue(request.body().contains("\"input\":\"Generate only Java statements for the method body."));
            assertTrue(request.body().contains("Contract source:"));
            assertTrue(request.body().contains("If the provided contract context is insufficient to implement real behavior"));
            assertTrue(request.body().contains("AIMP could not synthesize a concrete implementation for com.example.payment.PaymentService#charge."));
            assertTrue(request.body().contains("public interface PaymentService"));
            assertTrue(request.body().contains("@AIImplemented(\\\"Charge a payment and return the result\\\")"));
            return openAiOutputText("return new com.example.payment.PaymentResult(\"approved-openai\")");
        })) {
            result = ProcessorCompilation.compile(
                tempDir.resolve("openai-project"),
                new AimpProcessor(),
                List.of(
                    SourceFile.of("com/example/payment/PaymentRequest.java", """
                        package com.example.payment;

                        public record PaymentRequest(String reference) {
                        }
                        """),
                    SourceFile.of("com/example/payment/PaymentResult.java", """
                        package com.example.payment;

                        public record PaymentResult(String status) {
                        }
                        """),
                    SourceFile.of("com/example/payment/PaymentService.java", """
                        package com.example.payment;

                        import com.aimp.annotations.AIImplemented;

                        public interface PaymentService {
                            @AIImplemented("Charge a payment and return the result")
                            PaymentResult charge(PaymentRequest request);
                        }
                        """)
                ),
                Map.of(),
                server.processorOptions()
            );
        }

        assertTrue(result.success(), () -> String.join("\n", result.messages(javax.tools.Diagnostic.Kind.ERROR)));
        assertTrue(result.generatedSource("com/example/payment/PaymentService_AIGenerated.java")
            .contains("return new com.example.payment.PaymentResult(\"approved-openai\");"));
        assertTrue(
            result.messages(Diagnostic.Kind.NOTE).stream()
                .anyMatch(message -> message.contains("AIMP calling OpenAI for com.example.payment.PaymentService#charge"))
        );
        assertTrue(
            result.messages(Diagnostic.Kind.NOTE).stream()
                .anyMatch(message -> message.contains("AIMP received OpenAI output for com.example.payment.PaymentService#charge"))
        );
    }

    @Test
    void executesSynthesizedImplementation() throws Exception {
        CompilationResult result;
        try (FakeOpenAiServer server = FakeOpenAiServer.start(request -> {
            assertTrue(request.body().contains("answer"));
            return openAiOutputText("return prompt + \"-generated\";");
        })) {
            result = ProcessorCompilation.compile(
                tempDir.resolve("runtime-project"),
                new AimpProcessor(),
                List.of(
                    SourceFile.of("com/example/echo/EchoService.java", """
                        package com.example.echo;

                        import com.aimp.annotations.AIImplemented;

                        public interface EchoService {
                            @AIImplemented("Return an LLM-generated answer")
                            String answer(String prompt);
                        }
                        """)
                ),
                Map.of(),
                server.processorOptions()
            );
        }

        assertTrue(result.success(), () -> String.join("\n", result.messages(javax.tools.Diagnostic.Kind.ERROR)));

        try (URLClassLoader classLoader = URLClassLoader.newInstance(
            new java.net.URL[]{result.classesDirectory().toUri().toURL()},
            AimpProcessorCompileTest.class.getClassLoader()
        )) {
            Class<?> generatedType = classLoader.loadClass("com.example.echo.EchoService_AIGenerated");
            Object service = generatedType.getDeclaredConstructor().newInstance();
            Object response = generatedType.getMethod("answer", String.class).invoke(service, "ping");
            assertEquals("ping-generated", response);
        }
    }

    @Test
    void generatesSubclassAndPropagatesAllowlistedAnnotations() throws Exception {
        CompilationResult result;
        try (FakeOpenAiServer server = FakeOpenAiServer.start(request -> {
            assertTrue(request.body().contains("OrderServiceBase"));
            return openAiOutputText("return new com.example.order.OrderResult(\"reserved\");");
        })) {
            result = ProcessorCompilation.compile(
                tempDir.resolve("abstract-project"),
                new AimpProcessor(),
                List.of(
                    SourceFile.of("org/springframework/stereotype/Service.java", """
                        package org.springframework.stereotype;

                        import java.lang.annotation.ElementType;
                        import java.lang.annotation.Retention;
                        import java.lang.annotation.RetentionPolicy;
                        import java.lang.annotation.Target;

                        @Target(ElementType.TYPE)
                        @Retention(RetentionPolicy.RUNTIME)
                        public @interface Service {
                        }
                        """),
                    SourceFile.of("org/springframework/transaction/annotation/Transactional.java", """
                        package org.springframework.transaction.annotation;

                        import java.lang.annotation.ElementType;
                        import java.lang.annotation.Retention;
                        import java.lang.annotation.RetentionPolicy;
                        import java.lang.annotation.Target;

                        @Target(ElementType.METHOD)
                        @Retention(RetentionPolicy.RUNTIME)
                        public @interface Transactional {
                        }
                        """),
                    SourceFile.of("jakarta/validation/Valid.java", """
                        package jakarta.validation;

                        import java.lang.annotation.ElementType;
                        import java.lang.annotation.Retention;
                        import java.lang.annotation.RetentionPolicy;
                        import java.lang.annotation.Target;

                        @Target(ElementType.PARAMETER)
                        @Retention(RetentionPolicy.RUNTIME)
                        public @interface Valid {
                        }
                        """),
                    SourceFile.of("com/example/order/OrderRequest.java", """
                        package com.example.order;

                        public record OrderRequest(String id) {
                        }
                        """),
                    SourceFile.of("com/example/order/OrderResult.java", """
                        package com.example.order;

                        public record OrderResult(String status) {
                        }
                        """),
                    SourceFile.of("com/example/order/OrderServiceBase.java", """
                        package com.example.order;

                        import com.aimp.annotations.AIImplemented;
                        import jakarta.validation.Valid;
                        import org.springframework.stereotype.Service;
                        import org.springframework.transaction.annotation.Transactional;

                        @Service
                        public abstract class OrderServiceBase {
                            protected OrderServiceBase(String region) {
                            }

                            @AIImplemented("Reserve inventory and return the result")
                            @Transactional
                            public abstract OrderResult place(@Valid OrderRequest request);
                        }
                        """)
                ),
                Map.of("aimp.yml", """
                    aimp:
                      propagation:
                        annotations:
                          - org.springframework.stereotype.Service
                          - org.springframework.transaction.annotation.Transactional
                          - jakarta.validation.Valid
                    """),
                server.processorOptions()
            );
        }

        assertTrue(result.success(), () -> String.join("\n", result.messages(javax.tools.Diagnostic.Kind.ERROR)));

        String generated = normalize(result.generatedSource("com/example/order/OrderServiceBase_AIGenerated.java"));
        assertTrue(generated.contains("@org.springframework.stereotype.Service"));
        assertTrue(generated.contains("@org.springframework.transaction.annotation.Transactional"));
        assertTrue(generated.contains("@jakarta.validation.Valid com.example.order.OrderRequest request"));
        assertTrue(generated.contains("protected OrderServiceBase_AIGenerated(java.lang.String region)"));
        assertTrue(generated.contains("return new com.example.order.OrderResult(\"reserved\");"));
        assertFalse(generated.contains("@com.aimp.annotations.AIImplemented"));
    }

    @Test
    void invalidConcreteMethodFails() throws Exception {
        CompilationResult result = ProcessorCompilation.compile(
            tempDir.resolve("invalid-project"),
            new AimpProcessor(),
            List.of(
                SourceFile.of("com/example/bad/BadService.java", """
                    package com.example.bad;

                    import com.aimp.annotations.AIImplemented;

                    public abstract class BadService {
                        @AIImplemented("Should fail")
                        public String value() {
                            return "bad";
                        }
                    }
                    """)
            )
        );

        assertFalse(result.success());
        assertTrue(
            result.messages(javax.tools.Diagnostic.Kind.ERROR).stream()
                .anyMatch(message -> message.contains("concrete methods"))
        );
    }

    private static String readResource(String resourceName) throws IOException {
        try (var inputStream = AimpProcessorCompileTest.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IOException("Missing test resource " + resourceName);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String normalize(String value) {
        return value.replace("\r\n", "\n").trim();
    }

    private static byte[] readAllBytes(java.io.InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        inputStream.transferTo(outputStream);
        return outputStream.toByteArray();
    }

    private record OpenAiRequest(String authorization, String body) {
    }

    private static final class FakeOpenAiServer implements AutoCloseable {
        private final HttpServer server;

        private FakeOpenAiServer(HttpServer server) {
            this.server = server;
        }

        static FakeOpenAiServer start(Function<OpenAiRequest, String> responder) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/v1/responses", exchange -> {
                byte[] requestBytes = readAllBytes(exchange.getRequestBody());
                String requestBody = new String(requestBytes, StandardCharsets.UTF_8);
                String authorization = exchange.getRequestHeaders().getFirst("Authorization");
                byte[] responseBytes = responder.apply(new OpenAiRequest(authorization, requestBody))
                    .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseBytes.length);
                exchange.getResponseBody().write(responseBytes);
                exchange.close();
            });
            server.start();
            return new FakeOpenAiServer(server);
        }

        List<String> processorOptions() {
            return List.of(
                "-Aaimp.synthesis.model=gpt-5",
                "-Aaimp.synthesis.apiKey=test-openai-key",
                "-Aaimp.synthesis.openai.baseUrl=http://127.0.0.1:" + server.getAddress().getPort()
            );
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    private static String openAiOutputText(String javaBody) {
        return """
            {
              "output": [
                {
                  "type": "message",
                  "role": "assistant",
                  "content": [
                    {
                      "type": "output_text",
                      "text": "%s",
                      "annotations": []
                    }
                  ]
                }
              ]
            }
            """.formatted(JsonStringFieldExtractor.escape(javaBody));
    }
}
