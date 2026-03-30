package com.aimp.core;

import com.aimp.model.MethodBodyPlan;
import java.util.List;

public final class LlmMethodBodyRenderer implements MethodBodyRenderer {
    @Override
    public List<String> render(MethodBodyPlan plan) {
        return plan.statements();
    }
}
