package com.aimp.core;

import com.aimp.model.ContractModel;
import com.aimp.model.MethodBodyPlan;
import com.aimp.model.MethodModel;

/**
 * Produces a method body plan for an annotated contract method.
 */
@FunctionalInterface
public interface MethodBodySynthesizer {
    /**
     * Synthesizes a generated method body for the given contract method.
     *
     * @param contract the handwritten contract
     * @param method the annotated method to implement
     * @return the synthesized method body plan
     */
    MethodBodyPlan synthesize(ContractModel contract, MethodModel method);
}
