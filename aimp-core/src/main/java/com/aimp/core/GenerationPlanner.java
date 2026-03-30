package com.aimp.core;

import com.aimp.config.AimpConfig;
import com.aimp.model.AnnotationUsage;
import com.aimp.model.ContractModel;
import com.aimp.model.GeneratedConstructorPlan;
import com.aimp.model.GeneratedElementKind;
import com.aimp.model.GeneratedMethodPlan;
import com.aimp.model.GeneratedTypePlan;
import com.aimp.model.MethodBodyPlan;
import com.aimp.model.ParameterModel;
import java.util.List;
import java.util.Set;

public final class GenerationPlanner {
    private final AnnotationPropagationDecider annotationPropagationDecider;
    private final MethodImplementationPlanner methodImplementationPlanner;

    public GenerationPlanner() {
        this(
            new AnnotationPropagationDecider(),
            new DefaultMethodImplementationPlanner(
                (contract, method) -> new MethodBodyPlan(
                    List.of("throw new UnsupportedOperationException(\"AIMP synthesis backend not configured\");")
                )
            )
        );
    }

    public GenerationPlanner(
        AnnotationPropagationDecider annotationPropagationDecider,
        MethodImplementationPlanner methodImplementationPlanner
    ) {
        this.annotationPropagationDecider = annotationPropagationDecider;
        this.methodImplementationPlanner = methodImplementationPlanner;
    }

    public GeneratedTypePlan plan(ContractModel contract, AimpConfig config) {
        Set<String> allowlist = config.annotationAllowlist();

        List<GeneratedConstructorPlan> constructors = contract.constructors().stream()
            .map(constructor -> new GeneratedConstructorPlan(constructor.visibility(), constructor.parameters(), constructor.thrownTypes()))
            .toList();

        List<GeneratedMethodPlan> methods = contract.methods().stream()
            .map(method -> new GeneratedMethodPlan(
                method.name(),
                method.returnType(),
                method.visibility(),
                method.typeParameters(),
                method.parameters().stream()
                    .map(parameter -> new ParameterModel(
                        parameter.name(),
                        parameter.type(),
                        parameter.varArgs(),
                        propagate(parameter.annotations(), allowlist, GeneratedElementKind.PARAMETER)
                    ))
                    .toList(),
                method.thrownTypes(),
                propagate(method.annotations(), allowlist, GeneratedElementKind.METHOD),
                methodImplementationPlanner.plan(contract, method)
            ))
            .toList();

        return new GeneratedTypePlan(
            contract.packageName(),
            GeneratedTypeNaming.generatedSimpleName(contract.simpleName()),
            GeneratedTypeNaming.generatedQualifiedName(contract.packageName(), contract.simpleName()),
            contract.simpleName(),
            contract.kind(),
            contract.visibility(),
            contract.typeParameters(),
            propagate(contract.annotations(), allowlist, GeneratedElementKind.TYPE),
            constructors,
            methods
        );
    }

    private List<AnnotationUsage> propagate(
        List<AnnotationUsage> annotations,
        Set<String> allowlist,
        GeneratedElementKind targetKind
    ) {
        return annotationPropagationDecider.propagate(annotations, allowlist, targetKind);
    }
}
