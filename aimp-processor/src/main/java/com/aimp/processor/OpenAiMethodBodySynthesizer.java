package com.aimp.processor;

import com.aimp.core.MethodBodySynthesizer;
import com.aimp.model.ContractModel;
import com.aimp.model.MethodBodyPlan;
import com.aimp.model.MethodModel;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

final class OpenAiMethodBodySynthesizer implements MethodBodySynthesizer {
    private static final String DEFAULT_REASONING_EFFORT = "low";

    private final HttpClient httpClient;
    private final URI endpoint;
    private final String model;
    private final Duration timeout;
    private final String apiKey;
    private final Consumer<String> logger;

    OpenAiMethodBodySynthesizer(URI endpoint, String model, Duration timeout, String apiKey, Consumer<String> logger) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(timeout)
            .build();
        this.endpoint = endpoint;
        this.model = model;
        this.timeout = timeout;
        this.apiKey = apiKey;
        this.logger = logger;
    }

    @Override
    public MethodBodyPlan synthesize(ContractModel contract, MethodModel method) {
        String methodReference = contract.qualifiedName() + "#" + method.name();
        long startedAt = System.nanoTime();
        logger.accept("AIMP calling OpenAI for " + methodReference + " with model " + model);

        HttpRequest request = HttpRequest.newBuilder(endpoint)
            .timeout(timeout)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody(contract, method)))
            .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logger.accept("AIMP OpenAI call failed for " + methodReference + " after " + elapsedMillis(startedAt) + " ms");
            throw new MethodBodySynthesisException("Failed to call OpenAI Responses API at " + endpoint, exception);
        } catch (IOException exception) {
            logger.accept("AIMP OpenAI call failed for " + methodReference + " after " + elapsedMillis(startedAt) + " ms");
            throw new MethodBodySynthesisException("Failed to call OpenAI Responses API at " + endpoint, exception);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            logger.accept(
                "AIMP OpenAI call failed for "
                    + methodReference
                    + " with HTTP "
                    + response.statusCode()
                    + " after "
                    + elapsedMillis(startedAt)
                    + " ms"
            );
            throw new MethodBodySynthesisException(
                "OpenAI Responses API returned HTTP " + response.statusCode() + ": " + response.body()
            );
        }

        String body = OpenAiResponsesParser.extractOutputText(response.body());
        String sanitized = MethodBodyTextSanitizer.sanitize(body);
        if (sanitized.isBlank()) {
            throw new MethodBodySynthesisException("OpenAI synthesis returned an empty method body.");
        }
        logger.accept("AIMP received OpenAI output for " + methodReference + " in " + elapsedMillis(startedAt) + " ms");
        return new MethodBodyPlan(sanitized.lines().toList());
    }

    private static long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private String requestBody(ContractModel contract, MethodModel method) {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        appendField(builder, "model", model);
        builder.append(',');
        builder.append("\"reasoning\":{");
        appendField(builder, "effort", DEFAULT_REASONING_EFFORT);
        builder.append('}');
        builder.append(',');
        appendField(builder, "instructions", synthesisInstructions());
        builder.append(',');
        appendField(builder, "input", MethodBodySynthesisPromptFactory.prompt(contract, method));
        builder.append('}');
        return builder.toString();
    }

    private static String synthesisInstructions() {
        return "You synthesize Java method bodies for annotation-processor code generation. "
            + "Return only valid Java statements for the method body, with no markdown, no code fences, "
            + "no surrounding braces, and no explanatory prose. Terminate every non-block statement with a semicolon. "
            + "Do not emit generic placeholders such as TODO, stub, or Not implemented. "
            + "If the provided contract context is insufficient, use the exact fallback throw statement requested in the prompt.";
    }

    private static void appendField(StringBuilder builder, String name, String value) {
        builder.append('"').append(JsonStringFieldExtractor.escape(name)).append('"');
        builder.append(':');
        builder.append('"').append(JsonStringFieldExtractor.escape(value)).append('"');
    }
}
