package com.aimp.processor;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.aimp.core.GeneratedTypeNaming;
import com.aimp.model.ConstructorModel;
import com.aimp.model.ContractKind;
import com.aimp.model.ContractModel;
import com.aimp.model.MethodModel;
import com.aimp.model.ParameterModel;
import com.aimp.model.ReferencedTypeModel;
import com.aimp.model.TypeParameterModel;
import com.aimp.model.Visibility;
import java.util.List;
import java.util.stream.Collectors;

final class GeneratedClassSynthesisPromptFactory {
    private GeneratedClassSynthesisPromptFactory() {
    }

    static String prompt(
        ContractModel contract,
        List<ReferencedTypeModel> includedReferencedTypes,
        ContextRequestFeedback contextRequestFeedback,
        int roundNumber,
        int maxRounds
    ) {
        ObjectNode root = JsonSupport.objectNode();
        root.put("protocolVersion", GeneratedClassSynthesisProtocol.PROTOCOL_VERSION);
        root.put("task", "generate_generated_class");

        ObjectNode round = root.putObject("round");
        round.put("current", roundNumber);
        round.put("max", maxRounds);

        ObjectNode generationTarget = root.putObject("generationTarget");
        generationTarget.put("packageName", contract.packageName());
        generationTarget.put("generatedSimpleName", GeneratedTypeNaming.generatedSimpleName(contract.simpleName()));
        generationTarget.put(
            "generatedQualifiedName",
            GeneratedTypeNaming.generatedQualifiedName(contract.packageName(), contract.simpleName())
        );
        generationTarget.put("relationship", contract.kind() == ContractKind.INTERFACE ? "implement" : "extend");
        generationTarget.put("contractKind", contract.kind().name());
        generationTarget.put("contractQualifiedName", contract.qualifiedName());
        generationTarget.put("contractVersion", contract.version());

        ObjectNode responseContract = root.putObject("responseContract");
        responseContract.put("protocolVersion", GeneratedClassSynthesisProtocol.PROTOCOL_VERSION);
        ArrayNode responseTypeValues = responseContract.putArray("responseTypeValues");
        responseTypeValues.add(GeneratedClassSynthesisProtocol.RESPONSE_TYPE_GENERATED_CLASS);
        responseTypeValues.add(GeneratedClassSynthesisProtocol.RESPONSE_TYPE_REQUEST_CONTEXT_TYPES);
        boolean finalRound = roundNumber >= maxRounds;
        if (finalRound) {
            responseTypeValues.add(GeneratedClassSynthesisProtocol.RESPONSE_TYPE_INSUFFICIENT_CONTEXT);
        }
        responseContract.put(
            "generatedClassSourceRule",
            "Use when responseType is generated_class. Return the full Java source file for the generated class."
        );
        responseContract.put(
            "requestedTypeNamesRule",
            "Use when responseType is request_context_types. Request only concrete fully qualified Java type names whose source context would materially help."
        );
        if (finalRound) {
            responseContract.put(
                "callerMessageRule",
                "Use when responseType is insufficient_context. Ask the caller for missing business rules, examples, or handwritten code."
            );
        }

        ArrayNode constraints = root.putArray("constraints");
        addAll(
            constraints,
            List.of(
                "Return exactly one JSON object and nothing else.",
                "Do not return markdown or code fences.",
                "Do not include @AIImplemented anywhere in generatedClassSource.",
                "Do not include @AIContract anywhere in generatedClassSource.",
                "Generate a concrete, non-final class for the requested generatedSimpleName.",
                "generatedClassSource must compile as a standalone Java source file in the target package.",
                "For types outside java.lang and outside the generated package, add the required imports or use fully qualified names.",
                "Preserve parameter names, varargs, type parameters, return types, and declared exceptions.",
                "If the contract is an abstract class, generate constructors for every accessible superclass constructor and delegate to super(...).",
                "You may add private constants, fields, and helper methods when needed.",
                "Do not use reflection, method-name scanning, Method.invoke, or other dynamic invocation unless the contract explicitly requires it.",
                "Copy framework or validation annotations only when they are needed on the generated implementation.",
                "Do not duplicate annotations or mix declaration annotations with equivalent type-use annotations on the same element.",
                "Do not rely on private members of referenced types.",
                "If implementation depends on a collaborator API that is not present in contractSource or includedTypeContexts, request more context instead of guessing or using dynamic invocation.",
                "When requesting more context, request only fully qualified Java type names. Do not request methods, fields, packages, wildcards, or prose descriptions.",
                "AIMP validates requested types after each round and reports fulfilled and rejected requests in contextRequestFeedback.",
                "Prefer requesting types directly referenced from contractSource or from the source code already present in includedTypeContexts.",
                finalRound
                    ? "Use responseType insufficient_context only in this final round when the current context still cannot support a safe implementation."
                    : "Do not return responseType insufficient_context before the final round. In non-final rounds, return generated_class or request_context_types.",
                "Use responseType insufficient_context only when additional requested type context will not solve the missing context."
            )
        );

        root.put("contractSource", contractSource(contract));

        ArrayNode includedTypeContexts = root.putArray("includedTypeContexts");
        includedReferencedTypes.forEach(referencedType -> includedTypeContexts.add(referencedTypeNode(referencedType)));

        if (contextRequestFeedback != null && !contextRequestFeedback.isEmpty()) {
            ObjectNode feedback = root.putObject("contextRequestFeedback");
            addAll(feedback.putArray("fulfilledTypeNames"), contextRequestFeedback.fulfilledTypeNames());
            ArrayNode rejectedTypeRequests = feedback.putArray("rejectedTypeRequests");
            contextRequestFeedback.rejectedTypeRequests().forEach(rejectedRequest -> {
                ObjectNode rejected = rejectedTypeRequests.addObject();
                rejected.put("qualifiedName", rejectedRequest.qualifiedName());
                rejected.put("reason", rejectedRequest.reason());
            });
        }

        return JsonSupport.writeJson(root, "generated class synthesis prompt");
    }
    private static ObjectNode referencedTypeNode(ReferencedTypeModel referencedType) {
        ObjectNode json = JsonSupport.objectNode();
        json.put("qualifiedName", referencedType.qualifiedName());
        json.put("layer", referencedType.layer());
        json.put("source", referencedType.sourceSnippet());
        return json;
    }

    private static void addAll(ArrayNode array, List<String> values) {
        values.forEach(array::add);
    }

    private static String contractSource(ContractModel contract) {
        if (!contract.sourceSnippet().isBlank()) {
            return contract.sourceSnippet();
        }

        StringBuilder builder = new StringBuilder();
        contract.annotations().forEach(annotation -> builder.append(annotation.renderedSource()).append('\n'));
        builder.append(renderVisibility(contract.visibility()));
        builder.append(contract.kind() == ContractKind.INTERFACE ? "interface " : "abstract class ");
        builder.append(contract.simpleName());
        builder.append(renderTypeParameters(contract.typeParameters()));
        builder.append(" {\n");
        for (ConstructorModel constructor : contract.constructors()) {
            builder.append("    ")
                .append(renderVisibility(constructor.visibility()))
                .append(contract.simpleName())
                .append(renderParameters(constructor.parameters()))
                .append(renderThrows(constructor.thrownTypes()))
                .append(";\n");
        }
        for (MethodModel method : contract.methods()) {
            method.annotations().forEach(annotation -> builder.append("    ").append(annotation.renderedSource()).append('\n'));
            builder.append("    ")
                .append(renderVisibility(method.visibility()))
                .append(renderTypeParameters(method.typeParameters()));
            if (!method.typeParameters().isEmpty()) {
                builder.append(' ');
            }
            builder.append(method.returnType())
                .append(' ')
                .append(method.name())
                .append(renderParameters(method.parameters()))
                .append(renderThrows(method.thrownTypes()))
                .append(";\n");
        }
        builder.append('}');
        return builder.toString();
    }

    private static String renderVisibility(Visibility visibility) {
        return visibility.keyword().isBlank() ? "" : visibility.keyword() + " ";
    }

    private static String renderTypeParameters(java.util.List<TypeParameterModel> typeParameters) {
        if (typeParameters.isEmpty()) {
            return "";
        }
        return typeParameters.stream()
            .map(TypeParameterModel::declaration)
            .collect(Collectors.joining(", ", "<", ">"));
    }

    private static String renderParameters(java.util.List<ParameterModel> parameters) {
        return parameters.stream()
            .map(parameter -> {
                StringBuilder rendered = new StringBuilder();
                parameter.annotations().forEach(annotation -> rendered.append(annotation.renderedSource()).append(" "));
                String parameterType = parameter.varArgs() && parameter.type().endsWith("[]")
                    ? parameter.type().substring(0, parameter.type().length() - 2) + "..."
                    : parameter.type();
                rendered.append(parameterType).append(' ').append(parameter.name());
                return rendered.toString();
            })
            .collect(Collectors.joining(", ", "(", ")"));
    }

    private static String renderThrows(java.util.List<String> thrownTypes) {
        if (thrownTypes.isEmpty()) {
            return "";
        }
        return thrownTypes.stream().collect(Collectors.joining(", ", " throws ", ""));
    }
}
