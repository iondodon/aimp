package com.aimp.core;

import com.aimp.model.ContractModel;
import com.aimp.model.MethodModel;
import com.aimp.model.MethodBodyPlan;

/**
 * Plans a generated method implementation for a handwritten contract method.
 */
public interface MethodImplementationPlanner {
    /**
     * Produces a method body plan for the given contract method.
     *
     * @param contract the handwritten contract containing the method
     * @param method the annotated method to implement
     * @return the generated method body plan
     */
    MethodBodyPlan plan(ContractModel contract, MethodModel method);
}
