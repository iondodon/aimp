package com.aimp.processor;

import com.aimp.core.GeneratedTypeNaming;
import com.aimp.model.ContractModel;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

final class OpenAiGeneratedClassSynthesizer implements GeneratedClassSynthesizer {
    private static final String DEFAULT_REASONING_EFFORT = "low";

    private final HttpClient httpClient;
    private final URI endpoint;
    private final String model;
    private final Duration timeout;
    private final String apiKey;
    private final Consumer<String> logger;

    OpenAiGeneratedClassSynthesizer(URI endpoint, String model, Duration timeout, String apiKey, Consumer<String> logger) {
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
    public String synthesize(ContractModel contract) {
        String generatedQualifiedName = GeneratedTypeNaming.generatedQualifiedName(contract.packageName(), contract.simpleName());
        long startedAt = System.nanoTime();
        logger.accept("AIMP calling OpenAI for " + generatedQualifiedName + " with model " + model);

        HttpRequest request = HttpRequest.newBuilder(endpoint)
            .timeout(timeout)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody(contract)))
            .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logger.accept("AIMP OpenAI call failed for " + generatedQualifiedName + " after " + elapsedMillis(startedAt) + " ms");
            throw new MethodBodySynthesisException("Failed to call OpenAI Responses API at " + endpoint, exception);
        } catch (IOException exception) {
            logger.accept("AIMP OpenAI call failed for " + generatedQualifiedName + " after " + elapsedMillis(startedAt) + " ms");
            throw new MethodBodySynthesisException("Failed to call OpenAI Responses API at " + endpoint, exception);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            logger.accept(
                "AIMP OpenAI call failed for "
                    + generatedQualifiedName
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

        String outputText = OpenAiResponsesParser.extractOutputText(response.body());
        String sanitized = GeneratedClassSourceSanitizer.sanitize(outputText, contract);
        logger.accept("AIMP received OpenAI output for " + generatedQualifiedName + " in " + elapsedMillis(startedAt) + " ms");
        return sanitized;
    }

    private static long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private String requestBody(ContractModel contract) {
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
        appendField(builder, "input", GeneratedClassSynthesisPromptFactory.prompt(contract));
        builder.append('}');
        return builder.toString();
    }

    private static String synthesisInstructions() {
        return "You synthesize full Java implementation classes for annotation-processor code generation. "
            + "Return only valid Java source for the complete generated class, with no markdown, no code fences, "
            + "and no explanatory prose. Output exactly one top-level class in the requested package with the requested name. "
            + "Do not include @AIImplemented in the result. Avoid duplicate annotations.";
    }

    private static void appendField(StringBuilder builder, String name, String value) {
        builder.append('"').append(JsonStringFieldExtractor.escape(name)).append('"');
        builder.append(':');
        builder.append('"').append(JsonStringFieldExtractor.escape(value)).append('"');
    }
}
