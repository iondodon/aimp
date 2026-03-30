package com.aimp.core;

import com.aimp.model.ContractModel;
import com.aimp.model.MethodModel;
import com.aimp.model.MethodBodyPlan;

public interface MethodImplementationPlanner {
    MethodBodyPlan plan(ContractModel contract, MethodModel method);
}
