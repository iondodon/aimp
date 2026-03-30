package com.aimp.processor;

import com.aimp.core.GeneratedTypeNaming;
import com.aimp.model.ConstructorModel;
import com.aimp.model.ContractKind;
import com.aimp.model.ContractModel;
import com.aimp.model.MethodModel;
import com.aimp.model.ParameterModel;
import com.aimp.model.ReferencedTypeModel;
import com.aimp.model.TypeParameterModel;
import com.aimp.model.Visibility;
import java.util.stream.Collectors;

final class GeneratedClassSynthesisPromptFactory {
    private GeneratedClassSynthesisPromptFactory() {
    }

    static String prompt(ContractModel contract) {
        StringBuilder builder = new StringBuilder();
        builder.append("Generate the complete Java source file for the generated implementation class.\n");
        builder.append("Return only raw Java source. Do not return markdown, code fences, or explanations.\n");
        builder.append("Generate exactly one top-level class named ")
            .append(GeneratedTypeNaming.generatedSimpleName(contract.simpleName()))
            .append(" in package ")
            .append(contract.packageName())
            .append(".\n");
        builder.append("The generated class must ")
            .append(contract.kind() == ContractKind.INTERFACE ? "implement " : "extend ")
            .append(contract.qualifiedName())
            .append(".\n");
        builder.append("Override every method annotated with @AIImplemented.\n");
        builder.append("Preserve parameter names, varargs, type parameters, return types, and declared exceptions.\n");
        builder.append("If the contract is an abstract class, generate constructors for every accessible superclass constructor and delegate to super(...).\n");
        builder.append("Do not include @AIImplemented anywhere in the generated class.\n");
        builder.append("You may add private constants, fields, and helper methods when needed.\n");
        builder.append("If the handwritten contract contains framework or validation annotations that are needed on the generated implementation, copy them once to the legally applicable generated element.\n");
        builder.append("Do not duplicate annotations. Do not emit both a declaration annotation and an equivalent type-use annotation for the same element.\n");
        builder.append("Keep the output compilable as plain Java 21 source.\n");
        builder.append("If the contract context is insufficient to implement real behavior, do not invent hidden infrastructure.\n");
        builder.append("In that case, return exactly this single line and nothing else:\n");
        builder.append(GeneratedClassSourceSanitizer.INSUFFICIENT_CONTEXT_SENTINEL).append('\n');
        builder.append("Do not generate a fallback Java class in that case.\n");
        builder.append("Generated class name: ").append(GeneratedTypeNaming.generatedSimpleName(contract.simpleName())).append('\n');
        builder.append("Generated qualified name: ")
            .append(GeneratedTypeNaming.generatedQualifiedName(contract.packageName(), contract.simpleName()))
            .append('\n');
        builder.append("Contract kind: ").append(contract.kind()).append('\n');
        builder.append("Contract qualified name: ").append(contract.qualifiedName()).append('\n');
        builder.append("Annotated methods:\n");
        for (MethodModel method : contract.methods()) {
            builder.append("- ").append(method.returnType()).append(' ').append(method.name())
                .append(renderParameters(method.parameters()))
                .append(renderThrows(method.thrownTypes()))
                .append(" :: ")
                .append(method.description())
                .append('\n');
        }
        if (contract.constructors().isEmpty()) {
            builder.append("Accessible constructors: none\n");
        } else {
            builder.append("Accessible constructors:\n");
            for (ConstructorModel constructor : contract.constructors()) {
                builder.append("- ")
                    .append(renderVisibility(constructor.visibility()))
                    .append(contract.simpleName())
                    .append(renderParameters(constructor.parameters()))
                    .append(renderThrows(constructor.thrownTypes()))
                    .append('\n');
            }
        }
        builder.append("Contract source:\n");
        builder.append("```java\n");
        builder.append(contractSource(contract));
        builder.append("\n```\n");
        if (!contract.referencedTypes().isEmpty()) {
            builder.append("Referenced types with available source, including method signatures and accessible member context from the contract hierarchy:\n");
            for (ReferencedTypeModel referencedType : contract.referencedTypes()) {
                builder.append("Type: ").append(referencedType.qualifiedName()).append('\n');
                builder.append("```java\n");
                builder.append(referencedType.sourceSnippet());
                builder.append("\n```\n");
            }
        }
        return builder.toString();
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
