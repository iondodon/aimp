package com.aimp.core;

import com.aimp.model.MethodBodyPlan;
import java.util.List;

/**
 * Renders method body plans by returning their statement list as-is.
 */
public final class LlmMethodBodyRenderer implements MethodBodyRenderer {
    /**
     * Creates a renderer for synthesized method body statements.
     */
    public LlmMethodBodyRenderer() {
    }

    @Override
    public List<String> render(MethodBodyPlan plan) {
        return plan.statements();
    }
}
