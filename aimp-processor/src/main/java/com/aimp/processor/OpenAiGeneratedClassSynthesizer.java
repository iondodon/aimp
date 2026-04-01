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

    private final HttpClient httpClient;
    private final URI endpoint;
    private final String model;
    private final Duration timeout;
    private final String apiKey;
    private final int maxSynthesisRounds;
    private final Consumer<String> logger;

    OpenAiGeneratedClassSynthesizer(
        URI endpoint,
        String model,
        Duration timeout,
        String apiKey,
        int maxSynthesisRounds,
        Consumer<String> logger
    ) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(timeout)
            .build();
        this.endpoint = endpoint;
        this.model = model;
        this.timeout = timeout;
        this.apiKey = apiKey;
        this.maxSynthesisRounds = maxSynthesisRounds;
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
        LinkedHashSet<String> requestableTypeNames = contract.referencedTypes().stream()
            .filter(type -> type.layer() == 1)
            .map(ReferencedTypeModel::qualifiedName)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        ContextRequestFeedback contextRequestFeedback = null;

        for (int roundNumber = 1; roundNumber <= maxSynthesisRounds; roundNumber++) {
            String outputText = invokeOpenAi(
                contract,
                includedTypes(includedTypeNames, contextTypesByName),
                contextRequestFeedback,
                roundNumber,
                logTarget
            );

            GeneratedClassSynthesisProtocol.SynthesisResponse synthesisResponse =
                GeneratedClassSynthesisProtocol.parseResponse(outputText);
            switch (synthesisResponse.responseType()) {
                case GeneratedClassSynthesisProtocol.RESPONSE_TYPE_REQUEST_CONTEXT_TYPES -> {
                    contextRequestFeedback = handleRequestedContextTypes(
                        contract,
                        logTarget,
                        roundNumber,
                        synthesisResponse.requestedTypeNames(),
                        contextTypesByName,
                        includedTypeNames,
                        requestableTypeNames
                    );
                    continue;
                }
                case GeneratedClassSynthesisProtocol.RESPONSE_TYPE_INSUFFICIENT_CONTEXT -> {
                    if (roundNumber < maxSynthesisRounds) {
                        throw new MethodBodySynthesisException(
                            "OpenAI returned insufficient_context for "
                                + contract.qualifiedName()
                                + " before the final synthesis round "
                                + maxSynthesisRounds
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
                + maxSynthesisRounds
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

    private ContextRequestFeedback handleRequestedContextTypes(
        ContractModel contract,
        String logTarget,
        int roundNumber,
        List<String> requestedTypeNames,
        Map<String, ReferencedTypeModel> contextTypesByName,
        LinkedHashSet<String> includedTypeNames,
        LinkedHashSet<String> requestableTypeNames
    ) {
        if (requestedTypeNames.isEmpty()) {
            throw new MethodBodySynthesisException(
                "OpenAI requested more source-type context for "
                    + contract.qualifiedName()
                    + " but did not name any types. Request concrete fully qualified Java type names."
            );
        }
        if (roundNumber >= maxSynthesisRounds) {
            throw new MethodBodySynthesisException(
                "OpenAI requested more source-type context for "
                    + contract.qualifiedName()
                    + " after reaching the maximum of "
                    + maxSynthesisRounds
                    + " synthesis rounds. Add more context to @AIImplemented(\"...\") or the contract code."
            );
        }

        LinkedHashSet<String> deduplicatedRequests = new LinkedHashSet<>(requestedTypeNames);
        LinkedHashSet<String> currentRoundRequestableTypes = new LinkedHashSet<>(requestableTypeNames);
        List<String> fulfilledTypeNames = new java.util.ArrayList<>();
        List<RejectedContextTypeRequest> rejectedTypeRequests = new java.util.ArrayList<>();

        logger.accept(
            "AIMP OpenAI requested additional context for "
                + logTarget
                + " in round "
                + roundNumber
                + ": "
                + String.join(", ", deduplicatedRequests)
        );

        for (String requestedTypeName : deduplicatedRequests) {
            if (includedTypeNames.contains(requestedTypeName)) {
                rejectedTypeRequests.add(new RejectedContextTypeRequest(
                    requestedTypeName,
                    "AIMP already included this type in an earlier round."
                ));
                continue;
            }
            ReferencedTypeModel referencedType = contextTypesByName.get(requestedTypeName);
            if (referencedType == null) {
                rejectedTypeRequests.add(new RejectedContextTypeRequest(
                    requestedTypeName,
                    "AIMP could not supply this type because its source is unavailable or it is not reachable from the contract context graph."
                ));
                continue;
            }
            if (!currentRoundRequestableTypes.contains(requestedTypeName)) {
                rejectedTypeRequests.add(new RejectedContextTypeRequest(
                    requestedTypeName,
                    "AIMP could not supply this type in the current context step because it is not yet reachable from contractSource or the currently included type contexts."
                ));
                continue;
            }

            fulfilledTypeNames.add(requestedTypeName);
            includedTypeNames.add(requestedTypeName);
            requestableTypeNames.remove(requestedTypeName);
            currentRoundRequestableTypes.remove(requestedTypeName);
            referencedType.nextLayerTypeNames().stream()
                .filter(typeName -> !includedTypeNames.contains(typeName))
                .forEach(typeName -> {
                    currentRoundRequestableTypes.add(typeName);
                    requestableTypeNames.add(typeName);
                });
        }

        if (!rejectedTypeRequests.isEmpty()) {
            logger.accept(
                "AIMP could not provide some requested context for "
                    + logTarget
                    + " in round "
                    + roundNumber
                    + ": "
                    + rejectedTypeRequests.stream()
                        .map(rejected -> rejected.qualifiedName() + " (" + rejected.reason() + ")")
                        .collect(Collectors.joining(", "))
            );
        }

        if (requestableTypeNames.isEmpty() && roundNumber + 1 <= maxSynthesisRounds) {
            logger.accept(
                "AIMP exhausted additional source-type context layers for "
                    + logTarget
                    + " after round "
                    + roundNumber
            );
        }

        return new ContextRequestFeedback(fulfilledTypeNames, rejectedTypeRequests);
    }

    private String invokeOpenAi(
        ContractModel contract,
        List<ReferencedTypeModel> includedReferencedTypes,
        ContextRequestFeedback contextRequestFeedback,
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
                + maxSynthesisRounds
        );

        HttpRequest request = HttpRequest.newBuilder(endpoint)
            .timeout(timeout)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(
                requestBody(contract, includedReferencedTypes, contextRequestFeedback, roundNumber)
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
        ContextRequestFeedback contextRequestFeedback,
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
                contextRequestFeedback,
                roundNumber,
                maxSynthesisRounds
            )
        );
        return JsonSupport.writeJson(request, "OpenAI request body");
    }
}
