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
        List<String> availableNextLayerTypeNames,
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

        ObjectNode responseContract = root.putObject("responseContract");
        responseContract.put("protocolVersion", GeneratedClassSynthesisProtocol.PROTOCOL_VERSION);
        ArrayNode responseTypeValues = responseContract.putArray("responseTypeValues");
        responseTypeValues.add(GeneratedClassSynthesisProtocol.RESPONSE_TYPE_GENERATED_CLASS);
        responseTypeValues.add(GeneratedClassSynthesisProtocol.RESPONSE_TYPE_REQUEST_CONTEXT_TYPES);
        responseTypeValues.add(GeneratedClassSynthesisProtocol.RESPONSE_TYPE_INSUFFICIENT_CONTEXT);
        responseContract.put(
            "generatedClassSourceRule",
            "Use when responseType is generated_class. Return the full Java source file for the generated class."
        );
        responseContract.put(
            "requestedTypeNamesRule",
            "Use when responseType is request_context_types. Only request fully qualified names from availableNextLayerTypeNames."
        );
        responseContract.put(
            "callerMessageRule",
            "Use when responseType is insufficient_context. Ask the caller for missing business rules, examples, or handwritten code."
        );

        ArrayNode constraints = root.putArray("constraints");
        addAll(
            constraints,
            List.of(
                "Return exactly one JSON object and nothing else.",
                "Do not return markdown or code fences.",
                "Do not include @AIImplemented anywhere in generatedClassSource.",
                "Generate a concrete, non-final class for the requested generatedSimpleName.",
                "Preserve parameter names, varargs, type parameters, return types, and declared exceptions.",
                "If the contract is an abstract class, generate constructors for every accessible superclass constructor and delegate to super(...).",
                "You may add private constants, fields, and helper methods when needed.",
                "Copy framework or validation annotations only when they are needed on the generated implementation.",
                "Do not duplicate annotations or mix declaration annotations with equivalent type-use annotations on the same element.",
                "Do not rely on private members of referenced types.",
                "If availableNextLayerTypeNames is not empty and you need more source context, use responseType request_context_types before giving up.",
                "Use responseType insufficient_context only when additional type layers will not solve the missing context."
            )
        );

        ArrayNode annotatedMethods = root.putArray("annotatedMethods");
        contract.methods().forEach(method -> annotatedMethods.add(methodNode(method)));

        ArrayNode accessibleConstructors = root.putArray("accessibleConstructors");
        contract.constructors().forEach(constructor -> accessibleConstructors.add(constructorNode(constructor, contract.simpleName())));

        root.put("contractSource", contractSource(contract));

        ArrayNode includedTypeContexts = root.putArray("includedTypeContexts");
        includedReferencedTypes.forEach(referencedType -> includedTypeContexts.add(referencedTypeNode(referencedType)));

        ArrayNode availableNextLayerTypes = root.putArray("availableNextLayerTypeNames");
        addAll(availableNextLayerTypes, availableNextLayerTypeNames);

        return JsonSupport.writeJson(root, "generated class synthesis prompt");
    }

    private static ObjectNode methodNode(MethodModel method) {
        ObjectNode json = JsonSupport.objectNode();
        json.put("name", method.name());
        json.put("returnType", method.returnType());
        json.put("visibility", method.visibility().keyword());
        addAll(json.putArray("typeParameters"), method.typeParameters().stream().map(TypeParameterModel::declaration).toList());
        addAll(json.putArray("annotations"), method.annotations().stream().map(annotation -> annotation.renderedSource()).toList());
        addAll(json.putArray("thrownTypes"), method.thrownTypes());
        ArrayNode parameters = json.putArray("parameters");
        method.parameters().forEach(parameter -> parameters.add(parameterNode(parameter)));
        json.put("description", method.description());
        return json;
    }

    private static ObjectNode constructorNode(ConstructorModel constructor, String simpleName) {
        ObjectNode json = JsonSupport.objectNode();
        json.put("simpleName", simpleName);
        json.put("visibility", constructor.visibility().keyword());
        addAll(json.putArray("thrownTypes"), constructor.thrownTypes());
        ArrayNode parameters = json.putArray("parameters");
        constructor.parameters().forEach(parameter -> parameters.add(parameterNode(parameter)));
        return json;
    }

    private static ObjectNode parameterNode(ParameterModel parameter) {
        ObjectNode json = JsonSupport.objectNode();
        json.put("name", parameter.name());
        json.put("type", parameter.type());
        json.put("varArgs", parameter.varArgs());
        addAll(json.putArray("annotations"), parameter.annotations().stream().map(annotation -> annotation.renderedSource()).toList());
        return json;
    }

    private static ObjectNode referencedTypeNode(ReferencedTypeModel referencedType) {
        ObjectNode json = JsonSupport.objectNode();
        json.put("qualifiedName", referencedType.qualifiedName());
        json.put("layer", referencedType.layer());
        json.put("source", referencedType.sourceSnippet());
        addAll(json.putArray("nextLayerTypeNames"), referencedType.nextLayerTypeNames());
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
