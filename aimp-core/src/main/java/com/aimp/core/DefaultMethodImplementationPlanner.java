package com.aimp.core;

import com.aimp.model.ContractModel;
import com.aimp.model.MethodBodyPlan;
import com.aimp.model.MethodModel;

/**
 * Default planner that delegates method body synthesis to a {@link MethodBodySynthesizer}.
 */
public final class DefaultMethodImplementationPlanner implements MethodImplementationPlanner {
    private final MethodBodySynthesizer methodBodySynthesizer;

    /**
     * Creates a planner backed by the given synthesizer.
     *
     * @param methodBodySynthesizer the synthesizer used to produce method bodies
     */
    public DefaultMethodImplementationPlanner(MethodBodySynthesizer methodBodySynthesizer) {
        this.methodBodySynthesizer = methodBodySynthesizer;
    }

    @Override
    public MethodBodyPlan plan(ContractModel contract, MethodModel method) {
        return methodBodySynthesizer.synthesize(contract, method);
    }
}
