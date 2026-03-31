package com.aimp.processor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.aimp.core.GeneratedTypeNaming;
import com.aimp.model.ContractModel;
import com.aimp.model.ReferencedTypeModel;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

final class OpenAiGeneratedClassSynthesizer implements GeneratedClassSynthesizer {
    private static final String DEFAULT_REASONING_EFFORT = "low";
    private static final int MAX_SYNTHESIS_ROUNDS = 3;

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
        String contractQualifiedName = contract.qualifiedName();
        String generatedQualifiedName = GeneratedTypeNaming.generatedQualifiedName(contract.packageName(), contract.simpleName());
        String logTarget = "contract " + contractQualifiedName + " -> generated type " + generatedQualifiedName;
        Map<String, ReferencedTypeModel> contextTypesByName = contract.referencedTypes().stream()
            .collect(Collectors.toMap(ReferencedTypeModel::qualifiedName, type -> type, (left, right) -> left, LinkedHashMap::new));
        LinkedHashSet<String> includedTypeNames = new LinkedHashSet<>();
        LinkedHashSet<String> availableNextLayerTypeNames = contract.referencedTypes().stream()
            .filter(type -> type.layer() == 1)
            .map(ReferencedTypeModel::qualifiedName)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        for (int roundNumber = 1; roundNumber <= MAX_SYNTHESIS_ROUNDS; roundNumber++) {
            String outputText = invokeOpenAi(
                contract,
                includedTypes(includedTypeNames, contextTypesByName),
                List.copyOf(availableNextLayerTypeNames),
                roundNumber,
                logTarget
            );

            GeneratedClassSynthesisProtocol.SynthesisResponse synthesisResponse =
                GeneratedClassSynthesisProtocol.parseResponse(outputText);
            switch (synthesisResponse.responseType()) {
                case GeneratedClassSynthesisProtocol.RESPONSE_TYPE_REQUEST_CONTEXT_TYPES -> {
                    handleRequestedContextTypes(
                        contract,
                        logTarget,
                        roundNumber,
                        synthesisResponse.requestedTypeNames(),
                        contextTypesByName,
                        includedTypeNames,
                        availableNextLayerTypeNames
                    );
                    continue;
                }
                case GeneratedClassSynthesisProtocol.RESPONSE_TYPE_INSUFFICIENT_CONTEXT -> {
                    if (roundNumber < MAX_SYNTHESIS_ROUNDS) {
                        throw new MethodBodySynthesisException(
                            "OpenAI returned insufficient_context for "
                                + contract.qualifiedName()
                                + " before the final synthesis round "
                                + MAX_SYNTHESIS_ROUNDS
                                + ". "
                                + synthesisResponse.callerMessage()
                        );
                    }
                    throw new MethodBodySynthesisException(
                        "OpenAI requested more contract context for "
                            + contract.qualifiedName()
                            + ". "
                            + synthesisResponse.callerMessage()
                    );
                }
                case GeneratedClassSynthesisProtocol.RESPONSE_TYPE_GENERATED_CLASS ->
                    { return GeneratedClassSourceSanitizer.sanitize(synthesisResponse.generatedClassSource(), contract); }
                default -> throw new MethodBodySynthesisException(
                    "OpenAI synthesis response used unsupported responseType '" + synthesisResponse.responseType() + "'."
                );
            }
        }

        throw new MethodBodySynthesisException(
            "OpenAI requested more source-type context for "
                + contractQualifiedName
                + " after exhausting the maximum of "
                + MAX_SYNTHESIS_ROUNDS
                + " synthesis rounds. Add more context to @AIImplemented(\"...\") or the contract code."
        );
    }

    private static long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private List<ReferencedTypeModel> includedTypes(
        LinkedHashSet<String> includedTypeNames,
        Map<String, ReferencedTypeModel> contextTypesByName
    ) {
        return includedTypeNames.stream()
            .map(contextTypesByName::get)
            .filter(type -> type != null)
            .toList();
    }

    private void handleRequestedContextTypes(
        ContractModel contract,
        String logTarget,
        int roundNumber,
        List<String> requestedTypeNames,
        Map<String, ReferencedTypeModel> contextTypesByName,
        LinkedHashSet<String> includedTypeNames,
        LinkedHashSet<String> availableNextLayerTypeNames
    ) {
        if (requestedTypeNames.isEmpty()) {
            throw new MethodBodySynthesisException(
                "OpenAI requested more source-type context for "
                    + contract.qualifiedName()
                    + " but did not name any types. Request fully qualified names from the available next-layer list."
            );
        }
        if (roundNumber >= MAX_SYNTHESIS_ROUNDS) {
            throw new MethodBodySynthesisException(
                "OpenAI requested more source-type context for "
                    + contract.qualifiedName()
                    + " after reaching the maximum of "
                    + MAX_SYNTHESIS_ROUNDS
                    + " synthesis rounds. Add more context to @AIImplemented(\"...\") or the contract code."
            );
        }

        for (String requestedTypeName : requestedTypeNames) {
            if (!availableNextLayerTypeNames.contains(requestedTypeName)) {
                throw new MethodBodySynthesisException(
                    "OpenAI requested source type "
                        + requestedTypeName
                        + " for "
                        + contract.qualifiedName()
                        + ", but that type is not available in the next context layer."
                );
            }
            if (!contextTypesByName.containsKey(requestedTypeName)) {
                throw new MethodBodySynthesisException(
                    "OpenAI requested source type "
                        + requestedTypeName
                        + " for "
                        + contract.qualifiedName()
                        + ", but AIMP could not resolve source for that type."
                );
            }
        }

        logger.accept(
            "AIMP OpenAI requested additional context for "
                + logTarget
                + " in round "
                + roundNumber
                + ": "
                + String.join(", ", requestedTypeNames)
        );

        requestedTypeNames.forEach(typeName -> {
            includedTypeNames.add(typeName);
            availableNextLayerTypeNames.remove(typeName);
        });

        for (String requestedTypeName : requestedTypeNames) {
            ReferencedTypeModel referencedType = contextTypesByName.get(requestedTypeName);
            if (referencedType == null) {
                continue;
            }
            referencedType.nextLayerTypeNames().stream()
                .filter(typeName -> !includedTypeNames.contains(typeName))
                .forEach(availableNextLayerTypeNames::add);
        }

        if (availableNextLayerTypeNames.isEmpty() && roundNumber + 1 <= MAX_SYNTHESIS_ROUNDS) {
            logger.accept(
                "AIMP exhausted additional source-type context layers for "
                    + logTarget
                    + " after round "
                    + roundNumber
            );
        }
    }

    private String invokeOpenAi(
        ContractModel contract,
        List<ReferencedTypeModel> includedReferencedTypes,
        List<String> availableNextLayerTypeNames,
        int roundNumber,
        String logTarget
    ) {
        long startedAt = System.nanoTime();
        logger.accept(
            "AIMP invoking OpenAI for "
                + logTarget
                + " with model "
                + model
                + " in round "
                + roundNumber
                + " of "
                + MAX_SYNTHESIS_ROUNDS
        );

        HttpRequest request = HttpRequest.newBuilder(endpoint)
            .timeout(timeout)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(
                requestBody(contract, includedReferencedTypes, availableNextLayerTypeNames, roundNumber)
            ))
            .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logger.accept("AIMP OpenAI call failed for " + logTarget + " after " + elapsedMillis(startedAt) + " ms");
            throw new MethodBodySynthesisException("Failed to call OpenAI Responses API at " + endpoint, exception);
        } catch (IOException exception) {
            logger.accept("AIMP OpenAI call failed for " + logTarget + " after " + elapsedMillis(startedAt) + " ms");
            throw new MethodBodySynthesisException("Failed to call OpenAI Responses API at " + endpoint, exception);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            logger.accept(
                "AIMP OpenAI call failed for "
                    + logTarget
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
        logger.accept("AIMP received OpenAI output for " + logTarget + " in " + elapsedMillis(startedAt) + " ms");
        return outputText;
    }

    private String requestBody(
        ContractModel contract,
        List<ReferencedTypeModel> includedReferencedTypes,
        List<String> availableNextLayerTypeNames,
        int roundNumber
    ) {
        ObjectNode request = JsonSupport.objectNode();
        request.put("model", model);
        request.putObject("reasoning").put("effort", DEFAULT_REASONING_EFFORT);
        request.put("instructions", GeneratedClassSynthesisProtocol.instructionsJson());
        request.put(
            "input",
            GeneratedClassSynthesisPromptFactory.prompt(
                contract,
                includedReferencedTypes,
                availableNextLayerTypeNames,
                roundNumber,
                MAX_SYNTHESIS_ROUNDS
            )
        );
        return JsonSupport.writeJson(request, "OpenAI request body");
    }
}
