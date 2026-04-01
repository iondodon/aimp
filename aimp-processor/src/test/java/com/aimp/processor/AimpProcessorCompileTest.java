package com.aimp.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aimp.testkit.CompilationResult;
import com.aimp.testkit.ProcessorCompilation;
import com.aimp.testkit.SourceFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import javax.tools.Diagnostic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AimpProcessorCompileTest {
    private static final String PAYMENT_SERVICE_GENERATED_SOURCE = loadResource("golden/PaymentService_AIGenerated.java");

    @TempDir
    Path tempDir;

    @Test
    void generatesImplementationForInterface() throws Exception {
        CompilationResult result;
        try (FakeOpenAiServer server = FakeOpenAiServer.start(request -> {
            JsonNode requestJson = requestBodyJson(request);
            JsonNode inputJson = requestInputJson(request);
            assertEquals("Bearer test-openai-key", request.authorization());
            assertEquals("gpt-5", requestJson.path("model").asText());
            assertEquals("com.example.payment.PaymentService", inputJson.path("generationTarget").path("contractQualifiedName").asText());
            assertTrue(inputJson.path("contractSource").asText().contains("package com.example.payment;"));
            assertTrue(inputJson.path("contractSource").asText().contains("import com.aimp.annotations.AIImplemented;"));
            assertTrue(inputJson.path("contractSource").asText().contains("@AIImplemented(\"Charge a payment and return the result\")"));
            assertTrue(inputJson.path("annotatedMethods").isMissingNode());
            assertTrue(inputJson.path("accessibleConstructors").isMissingNode());
            assertTrue(inputJson.path("availableNextLayerTypeNames").isMissingNode());
            return openAiOutputText(openAiGeneratedClassResponse(PAYMENT_SERVICE_GENERATED_SOURCE));
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
            JsonNode requestJson = requestBodyJson(request);
            JsonNode instructionsJson = requestInstructionsJson(request);
            JsonNode inputJson = requestInputJson(request);
            assertEquals("Bearer test-openai-key", request.authorization());
            assertEquals("gpt-5", requestJson.path("model").asText());
            assertEquals("1", instructionsJson.path("protocolVersion").asText());
            assertEquals("generate_generated_class", inputJson.path("task").asText());
            assertEquals("PaymentService_AIGenerated", inputJson.path("generationTarget").path("generatedSimpleName").asText());
            assertTrue(inputJson.path("contractSource").asText().contains("public interface PaymentService"));
            assertTrue(inputJson.path("availableNextLayerTypeNames").isMissingNode());
            return openAiOutputText(openAiGeneratedClassResponse("""
                package com.example.payment;

                public class PaymentService_AIGenerated implements PaymentService {
                    @Override
                    public com.example.payment.PaymentResult charge(com.example.payment.PaymentRequest request) {
                        return new com.example.payment.PaymentResult("approved-openai");
                    }
                }
                """));
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
                .anyMatch(message -> message.contains(
                    "AIMP invoking OpenAI for contract com.example.payment.PaymentService -> generated type com.example.payment.PaymentService_AIGenerated"
                ))
        );
        assertTrue(
            result.messages(Diagnostic.Kind.NOTE).stream()
                .anyMatch(message -> message.contains(
                    "AIMP received OpenAI output for contract com.example.payment.PaymentService -> generated type com.example.payment.PaymentService_AIGenerated"
                ))
        );
    }

    @Test
    void executesSynthesizedImplementation() throws Exception {
        CompilationResult result;
        try (FakeOpenAiServer server = FakeOpenAiServer.start(request -> {
            JsonNode inputJson = requestInputJson(request);
            assertTrue(inputJson.path("contractSource").asText().contains("String answer(String prompt);"));
            assertTrue(inputJson.path("annotatedMethods").isMissingNode());
            assertTrue(inputJson.path("accessibleConstructors").isMissingNode());
            assertTrue(inputJson.path("availableNextLayerTypeNames").isMissingNode());
            return openAiOutputText(openAiGeneratedClassResponse("""
                package com.example.echo;

                public class EchoService_AIGenerated implements EchoService {
                    @Override
                    public java.lang.String answer(java.lang.String prompt) {
                        return prompt + "-generated";
                    }
                }
                """));
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
    void generatesSubclassWithoutAimpConfigAndLetsOpenAiDefineAnnotations() throws Exception {
        CompilationResult result;
        try (FakeOpenAiServer server = FakeOpenAiServer.start(request -> {
            JsonNode inputJson = requestInputJson(request);
            assertEquals("com.example.order.OrderServiceBase", inputJson.path("generationTarget").path("contractQualifiedName").asText());
            assertTrue(inputJson.path("contractSource").asText().contains("@Service"));
            assertTrue(inputJson.path("contractSource").asText().contains("@Transactional"));
            assertTrue(inputJson.path("contractSource").asText().contains("@Valid"));
            assertTrue(textArray(inputJson.path("constraints")).contains(
                "Copy framework or validation annotations only when they are needed on the generated implementation."
            ));
            return openAiOutputText(openAiGeneratedClassResponse("""
                package com.example.order;

                @org.springframework.stereotype.Service
                public class OrderServiceBase_AIGenerated extends OrderServiceBase {
                    protected OrderServiceBase_AIGenerated(java.lang.String region) {
                        super(region);
                    }

                    @Override
                    @org.springframework.transaction.annotation.Transactional
                    public com.example.order.OrderResult place(@jakarta.validation.Valid com.example.order.OrderRequest request) {
                        return new com.example.order.OrderResult("reserved");
                    }
                }
                """));
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

                        @Target({ElementType.PARAMETER, ElementType.TYPE_USE})
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
                Map.of(),
                server.processorOptions()
            );
        }

        assertTrue(result.success(), () -> String.join("\n", result.messages(javax.tools.Diagnostic.Kind.ERROR)));

        String generated = normalize(result.generatedSource("com/example/order/OrderServiceBase_AIGenerated.java"));
        assertTrue(generated.contains("@org.springframework.stereotype.Service"));
        assertTrue(generated.contains("@org.springframework.transaction.annotation.Transactional"));
        assertTrue(generated.contains("@jakarta.validation.Valid com.example.order.OrderRequest request"));
        assertFalse(generated.contains("com.example.order.@jakarta.validation.Valid OrderRequest"));
        assertTrue(generated.contains("protected OrderServiceBase_AIGenerated(java.lang.String region)"));
        assertTrue(generated.contains("return new com.example.order.OrderResult(\"reserved\");"));
        assertFalse(generated.contains("@com.aimp.annotations.AIImplemented"));
    }

    @Test
    void llmCanRequestDeeperTypesAcrossMultipleRounds() throws Exception {
        CompilationResult result;
        AtomicInteger requestCount = new AtomicInteger();
        try (FakeOpenAiServer server = FakeOpenAiServer.start(request -> {
            JsonNode inputJson = requestInputJson(request);
            int round = requestCount.incrementAndGet();
            if (round == 1) {
                assertTrue(inputJson.path("availableNextLayerTypeNames").isMissingNode());
                assertEquals(0, inputJson.path("includedTypeContexts").size());
                return openAiOutputText(openAiRequestContextTypesResponse(List.of("com.example.graph.B")));
            }

            if (round == 2) {
                assertSameElements(List.of("com.example.graph.B"), textValues(inputJson.path("includedTypeContexts").findValues("qualifiedName")));
                assertTrue(inputJson.path("includedTypeContexts").get(0).path("source").asText().contains("package com.example.graph;"));
                assertTrue(inputJson.path("includedTypeContexts").get(0).path("source").asText().contains("final class BSupport"));
                assertTrue(inputJson.path("includedTypeContexts").get(0).path("nextLayerTypeNames").isMissingNode());
                assertEquals(List.of("com.example.graph.B"), textArray(inputJson.path("contextRequestFeedback").path("fulfilledTypeNames")));
                return openAiOutputText(openAiRequestContextTypesResponse(List.of("com.example.graph.C")));
            }

            if (round == 3) {
                assertSameElements(
                    List.of("com.example.graph.B", "com.example.graph.C"),
                    textValues(inputJson.path("includedTypeContexts").findValues("qualifiedName"))
                );
                assertEquals(List.of("com.example.graph.C"), textArray(inputJson.path("contextRequestFeedback").path("fulfilledTypeNames")));
                assertTrue(inputJson.path("includedTypeContexts").get(1).path("source").asText().contains("public final class C"));
                return openAiOutputText(openAiRequestContextTypesResponse(List.of("com.example.graph.D")));
            }

            assertEquals(4, round);
            assertSameElements(
                List.of("com.example.graph.B", "com.example.graph.C", "com.example.graph.D"),
                textValues(inputJson.path("includedTypeContexts").findValues("qualifiedName"))
            );
            assertEquals(List.of("com.example.graph.D"), textArray(inputJson.path("contextRequestFeedback").path("fulfilledTypeNames")));
            return openAiOutputText(openAiGeneratedClassResponse("""
                package com.example.graph;

                public class GraphService_AIGenerated implements GraphService {
                    @Override
                    public java.lang.String describe(com.example.graph.B root) {
                        return root.c().d().value();
                    }
                }
                """));
        })) {
            result = ProcessorCompilation.compile(
                tempDir.resolve("graph-project"),
                new AimpProcessor(),
                List.of(
                    SourceFile.of("com/example/graph/B.java", """
                        package com.example.graph;

                        public final class B {
                            private final C c;

                            public B(C c) {
                                this.c = c;
                            }

                            public C c() {
                                return c;
                            }
                        }

                        final class BSupport {
                        }
                        """),
                    SourceFile.of("com/example/graph/C.java", """
                        package com.example.graph;

                        public final class C {
                            private final D d;

                            public C(D d) {
                                this.d = d;
                            }

                            public D d() {
                                return d;
                            }
                        }
                        """),
                    SourceFile.of("com/example/graph/D.java", """
                        package com.example.graph;

                        public record D(String value) {
                        }
                        """),
                    SourceFile.of("com/example/graph/GraphService.java", """
                        package com.example.graph;

                        import com.aimp.annotations.AIImplemented;

                        public interface GraphService {
                            @AIImplemented("Return root.c().d().value()")
                            String describe(B root);
                        }
                        """)
                ),
                Map.of(),
                server.processorOptions("-Aaimp.synthesis.maxRounds=5")
            );
        }

        assertTrue(result.success(), () -> String.join("\n", result.messages(javax.tools.Diagnostic.Kind.ERROR)));
        assertEquals(4, requestCount.get());
        assertTrue(
            result.messages(Diagnostic.Kind.NOTE).stream()
                .anyMatch(message -> message.contains(
                    "AIMP OpenAI requested additional context for contract com.example.graph.GraphService -> generated type com.example.graph.GraphService_AIGenerated in round 3"
                ))
        );
    }

    @Test
    void llmCanRequestHelperTypeWhenMethodSignatureHasNoSourceAvailableTypes() throws Exception {
        CompilationResult result;
        AtomicInteger requestCount = new AtomicInteger();
        try (FakeOpenAiServer server = FakeOpenAiServer.start(request -> {
            JsonNode inputJson = requestInputJson(request);
            int round = requestCount.incrementAndGet();
            assertTrue(inputJson.path("availableNextLayerTypeNames").isMissingNode());
            if (round == 1) {
                assertEquals(0, inputJson.path("includedTypeContexts").size());
                return openAiOutputText(openAiRequestContextTypesResponse(List.of("com.example.noargs.Helper")));
            }

            assertEquals(2, round);
            assertSameElements(List.of("com.example.noargs.Helper"), textValues(inputJson.path("includedTypeContexts").findValues("qualifiedName")));
            return openAiOutputText(openAiGeneratedClassResponse("""
                package com.example.noargs;

                public class StatusService_AIGenerated extends StatusService {
                    protected StatusService_AIGenerated(com.example.noargs.Helper helper) {
                        super(helper);
                    }

                    @Override
                    public java.lang.String status() {
                        return this.helper.status();
                    }
                }
                """));
        })) {
            result = ProcessorCompilation.compile(
                tempDir.resolve("noargs-project"),
                new AimpProcessor(),
                List.of(
                    SourceFile.of("com/example/noargs/Helper.java", """
                        package com.example.noargs;

                        public interface Helper {
                            String status();
                        }
                        """),
                    SourceFile.of("com/example/noargs/StatusService.java", """
                        package com.example.noargs;

                        import com.aimp.annotations.AIImplemented;

                        public abstract class StatusService {
                            protected final Helper helper;

                            protected StatusService(Helper helper) {
                                this.helper = helper;
                            }

                            @AIImplemented("Return the helper status")
                            public abstract String status();
                        }
                        """)
                ),
                Map.of(),
                server.processorOptions()
            );
        }

        assertTrue(result.success(), () -> String.join("\n", result.messages(javax.tools.Diagnostic.Kind.ERROR)));
        assertEquals(2, requestCount.get());
    }

    @Test
    void insufficientContextFailsCompilation() throws Exception {
        CompilationResult result;
        try (FakeOpenAiServer server = FakeOpenAiServer.start(request -> {
            JsonNode inputJson = requestInputJson(request);
            assertFalse(textArray(inputJson.path("responseContract").path("responseTypeValues"))
                .contains(GeneratedClassSynthesisProtocol.RESPONSE_TYPE_INSUFFICIENT_CONTEXT));
            assertTrue(inputJson.path("availableNextLayerTypeNames").isMissingNode());
            return openAiOutputText(openAiInsufficientContextResponse(
                "Add concrete approval and rejection examples for request.reference()."
            ));
        })) {
            result = ProcessorCompilation.compile(
                tempDir.resolve("insufficient-context-project"),
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
                server.processorOptions("-Aaimp.synthesis.maxRounds=4")
            );
        }

        assertFalse(result.success());
        assertTrue(
            result.messages(javax.tools.Diagnostic.Kind.ERROR).stream()
                .anyMatch(message -> message.contains(
                    "returned insufficient_context for com.example.payment.PaymentService before the final synthesis round 4"
                ))
        );
        assertTrue(
            result.messages(javax.tools.Diagnostic.Kind.ERROR).stream()
                .anyMatch(message -> message.contains("Add concrete approval and rejection examples for request.reference()."))
        );
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

    private static String loadResource(String resourceName) {
        try {
            return readResource(resourceName);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load test resource " + resourceName, exception);
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

    private static JsonNode requestBodyJson(OpenAiRequest request) {
        return JsonSupport.readTree(request.body(), "OpenAI request body");
    }

    private static JsonNode requestInputJson(OpenAiRequest request) {
        return JsonSupport.readTree(
            JsonSupport.requireText(requestBodyJson(request), "input", "OpenAI request body"),
            "OpenAI request input"
        );
    }

    private static JsonNode requestInstructionsJson(OpenAiRequest request) {
        return JsonSupport.readTree(
            JsonSupport.requireText(requestBodyJson(request), "instructions", "OpenAI request body"),
            "OpenAI request instructions"
        );
    }

    private static List<String> textArray(JsonNode node) {
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        node.forEach(item -> values.add(item.asText()));
        return List.copyOf(values);
    }

    private static List<String> textValues(List<JsonNode> nodes) {
        return nodes.stream().map(JsonNode::asText).toList();
    }

    private static void assertSameElements(List<String> expected, List<String> actual) {
        assertEquals(expected.size(), actual.size());
        assertEquals(new java.util.LinkedHashSet<>(expected), new java.util.LinkedHashSet<>(actual));
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

        List<String> processorOptions(String... extraOptions) {
            java.util.ArrayList<String> options = new java.util.ArrayList<>(List.of(
                "-Aaimp.synthesis.model=gpt-5",
                "-Aaimp.synthesis.apiKey=test-openai-key",
                "-Aaimp.synthesis.openai.baseUrl=http://127.0.0.1:" + server.getAddress().getPort()
            ));
            options.addAll(List.of(extraOptions));
            return List.copyOf(options);
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    private static String openAiOutputText(String modelOutput) {
        ObjectNode root = JsonSupport.objectNode();
        ArrayNode output = root.putArray("output");
        ObjectNode message = output.addObject();
        message.put("type", "message");
        message.put("role", "assistant");
        ArrayNode content = message.putArray("content");
        ObjectNode outputText = content.addObject();
        outputText.put("type", "output_text");
        outputText.put("text", modelOutput);
        outputText.putArray("annotations");
        return JsonSupport.writeJson(root, "fake OpenAI output");
    }

    private static String openAiGeneratedClassResponse(String javaSource) {
        ObjectNode root = JsonSupport.objectNode();
        root.put("protocolVersion", GeneratedClassSynthesisProtocol.PROTOCOL_VERSION);
        root.put("responseType", GeneratedClassSynthesisProtocol.RESPONSE_TYPE_GENERATED_CLASS);
        root.put("generatedClassSource", javaSource);
        return JsonSupport.writeJson(root, "generated class response");
    }

    private static String openAiRequestContextTypesResponse(List<String> requestedTypeNames) {
        ObjectNode root = JsonSupport.objectNode();
        root.put("protocolVersion", GeneratedClassSynthesisProtocol.PROTOCOL_VERSION);
        root.put("responseType", GeneratedClassSynthesisProtocol.RESPONSE_TYPE_REQUEST_CONTEXT_TYPES);
        ArrayNode requestedTypes = root.putArray("requestedTypeNames");
        requestedTypeNames.forEach(requestedTypes::add);
        return JsonSupport.writeJson(root, "request context types response");
    }

    private static String openAiInsufficientContextResponse(String callerMessage) {
        ObjectNode root = JsonSupport.objectNode();
        root.put("protocolVersion", GeneratedClassSynthesisProtocol.PROTOCOL_VERSION);
        root.put("responseType", GeneratedClassSynthesisProtocol.RESPONSE_TYPE_INSUFFICIENT_CONTEXT);
        root.put("callerMessage", callerMessage);
        return JsonSupport.writeJson(root, "insufficient context response");
    }
}
