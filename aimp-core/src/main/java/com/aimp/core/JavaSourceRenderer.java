package com.aimp.core;

import com.aimp.model.ContractKind;
import com.aimp.model.GeneratedConstructorPlan;
import com.aimp.model.GeneratedMethodPlan;
import com.aimp.model.GeneratedTypePlan;
import com.aimp.model.ParameterModel;
import com.aimp.model.TypeParameterModel;
import com.aimp.model.Visibility;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class JavaSourceRenderer {
    private final MethodBodyRenderer methodBodyRenderer;

    public JavaSourceRenderer() {
        this(new LlmMethodBodyRenderer());
    }

    public JavaSourceRenderer(MethodBodyRenderer methodBodyRenderer) {
        this.methodBodyRenderer = methodBodyRenderer;
    }

    public String render(GeneratedTypePlan plan) {
        JavaSourceWriter writer = new JavaSourceWriter();

        if (!plan.packageName().isBlank()) {
            writer.line("package " + plan.packageName() + ";");
            writer.blankLine();
        }

        plan.annotations().forEach(annotation -> writer.line(annotation.renderedSource()));

        StringBuilder declaration = new StringBuilder();
        if (plan.visibility() == Visibility.PUBLIC) {
            declaration.append("public ");
        }
        declaration.append("final class ").append(plan.simpleName());

        String typeParameters = renderTypeParameters(plan.typeParameters());
        if (!typeParameters.isBlank()) {
            declaration.append(typeParameters);
        }

        declaration.append(" ");
        declaration.append(plan.contractKind() == ContractKind.INTERFACE ? "implements " : "extends ");
        declaration.append(plan.contractSimpleName()).append(renderTypeArguments(plan.typeParameters()));

        writer.openBlock(declaration.toString());

        List<Runnable> memberRenderers = new ArrayList<>();
        for (GeneratedConstructorPlan constructor : plan.constructors()) {
            memberRenderers.add(() -> renderConstructor(writer, plan.simpleName(), constructor));
        }
        for (GeneratedMethodPlan method : plan.methods()) {
            memberRenderers.add(() -> renderMethod(writer, method));
        }

        for (int index = 0; index < memberRenderers.size(); index++) {
            memberRenderers.get(index).run();
            if (index + 1 < memberRenderers.size()) {
                writer.blankLine();
            }
        }

        writer.closeBlock();
        return writer.toString();
    }

    private void renderConstructor(JavaSourceWriter writer, String generatedSimpleName, GeneratedConstructorPlan plan) {
        writer.openBlock(renderVisibility(plan.visibility()) + generatedSimpleName + renderParameters(plan.parameters()) + renderThrows(plan.thrownTypes()));
        writer.line("super(" + plan.parameters().stream().map(ParameterModel::name).collect(Collectors.joining(", ")) + ");");
        writer.closeBlock();
    }

    private void renderMethod(JavaSourceWriter writer, GeneratedMethodPlan plan) {
        plan.annotations().forEach(annotation -> writer.line(annotation.renderedSource()));
        writer.line("@Override");
        String typeParameters = renderTypeParameters(plan.typeParameters());
        writer.openBlock(
            renderVisibility(plan.visibility())
                + (typeParameters.isBlank() ? "" : typeParameters + " ")
                + plan.returnType()
                + " "
                + plan.name()
                + renderParameters(plan.parameters())
                + renderThrows(plan.thrownTypes())
        );
        methodBodyRenderer.render(plan.bodyPlan()).forEach(writer::line);
        writer.closeBlock();
    }

    private static String renderVisibility(Visibility visibility) {
        return visibility.keyword().isBlank() ? "" : visibility.keyword() + " ";
    }

    private static String renderTypeParameters(List<TypeParameterModel> typeParameters) {
        if (typeParameters.isEmpty()) {
            return "";
        }
        return typeParameters.stream()
            .map(TypeParameterModel::declaration)
            .collect(Collectors.joining(", ", "<", ">"));
    }

    private static String renderTypeArguments(List<TypeParameterModel> typeParameters) {
        if (typeParameters.isEmpty()) {
            return "";
        }
        return typeParameters.stream()
            .map(TypeParameterModel::name)
            .collect(Collectors.joining(", ", "<", ">"));
    }

    private static String renderParameters(List<ParameterModel> parameters) {
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

    private static String renderThrows(List<String> thrownTypes) {
        if (thrownTypes.isEmpty()) {
            return "";
        }
        return thrownTypes.stream().collect(Collectors.joining(", ", " throws ", ""));
    }
}
