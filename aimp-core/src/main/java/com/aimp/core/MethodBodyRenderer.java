package com.aimp.core;

import com.aimp.model.MethodBodyPlan;
import java.util.List;

/**
 * Renders a method body plan into Java source lines.
 */
public interface MethodBodyRenderer {
    /**
     * Renders the supplied method body plan.
     *
     * @param plan the method body plan to render
     * @return the rendered Java source lines
     */
    List<String> render(MethodBodyPlan plan);
}
