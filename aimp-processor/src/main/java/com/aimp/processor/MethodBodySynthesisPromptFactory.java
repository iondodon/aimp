package com.aimp.processor;

import com.aimp.model.ConstructorModel;
import com.aimp.model.ContractModel;
import com.aimp.model.MethodModel;
import com.aimp.model.ParameterModel;
import com.aimp.model.TypeParameterModel;
import com.aimp.model.Visibility;
import java.util.stream.Collectors;

final class MethodBodySynthesisPromptFactory {
    private MethodBodySynthesisPromptFactory() {
    }

    static String prompt(ContractModel contract, MethodModel method) {
        StringBuilder builder = new StringBuilder();
        builder.append("Generate only Java statements for the method body.\n");
        builder.append("Do not return markdown, code fences, a method signature, or surrounding braces.\n");
        builder.append("Use the exact parameter names that were provided.\n");
        builder.append("Prefer fully qualified type names for referenced types.\n");
        builder.append("Use the full contract source below as the primary context.\n");
        builder.append("Contract type: ").append(contract.qualifiedName()).append('\n');
        builder.append("Contract kind: ").append(contract.kind()).append('\n');
        builder.append("Method name: ").append(method.name()).append('\n');
        builder.append("Return type: ").append(method.returnType()).append('\n');
        builder.append("Description: ").append(method.description()).append('\n');
        if (method.typeParameters().isEmpty()) {
            builder.append("Type parameters: none\n");
        } else {
            builder.append("Type parameters:\n");
            method.typeParameters().forEach(typeParameter -> builder.append("- ").append(typeParameter.declaration()).append('\n'));
        }
        if (method.parameters().isEmpty()) {
            builder.append("Parameters: none\n");
        } else {
            builder.append("Parameters:\n");
            method.parameters().forEach(parameter -> builder.append("- ").append(parameter.type()).append(' ').append(parameter.name()).append('\n'));
        }
        if (method.thrownTypes().isEmpty()) {
            builder.append("Thrown types: none\n");
        } else {
            builder.append("Thrown types:\n");
            method.thrownTypes().forEach(type -> builder.append("- ").append(type).append('\n'));
        }
        builder.append("Contract source:\n");
        builder.append("```java\n");
        builder.append(contractSource(contract));
        builder.append("\n```\n");
        return builder.toString();
    }

    private static String contractSource(ContractModel contract) {
        if (!contract.sourceSnippet().isBlank()) {
            return contract.sourceSnippet();
        }

        StringBuilder builder = new StringBuilder();
        contract.annotations().forEach(annotation -> builder.append(annotation.renderedSource()).append('\n'));
        builder.append(renderVisibility(contract.visibility()));
        builder.append(contract.kind() == com.aimp.model.ContractKind.INTERFACE ? "interface " : "abstract class ");
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
                builder.append(" ");
            }
            builder.append(method.returnType())
                .append(" ")
                .append(method.name())
                .append(renderParameters(method.parameters()))
                .append(renderThrows(method.thrownTypes()))
                .append(";\n");
        }
        builder.append("}");
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
                rendered.append(parameterType).append(" ").append(parameter.name());
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
