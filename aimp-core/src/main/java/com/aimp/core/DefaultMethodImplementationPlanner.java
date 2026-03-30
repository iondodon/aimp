package com.aimp.core;

import com.aimp.model.ContractModel;
import com.aimp.model.MethodBodyPlan;
import com.aimp.model.MethodModel;

public final class DefaultMethodImplementationPlanner implements MethodImplementationPlanner {
    private final MethodBodySynthesizer methodBodySynthesizer;

    public DefaultMethodImplementationPlanner(MethodBodySynthesizer methodBodySynthesizer) {
        this.methodBodySynthesizer = methodBodySynthesizer;
    }

    @Override
    public MethodBodyPlan plan(ContractModel contract, MethodModel method) {
        return methodBodySynthesizer.synthesize(contract, method);
    }
}
