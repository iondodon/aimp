package com.aimp.core;

import com.aimp.model.ContractModel;
import com.aimp.model.MethodBodyPlan;
import com.aimp.model.MethodModel;

@FunctionalInterface
public interface MethodBodySynthesizer {
    MethodBodyPlan synthesize(ContractModel contract, MethodModel method);
}
