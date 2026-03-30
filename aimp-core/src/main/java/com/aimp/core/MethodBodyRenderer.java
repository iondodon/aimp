package com.aimp.core;

import com.aimp.model.MethodBodyPlan;
import java.util.List;

public interface MethodBodyRenderer {
    List<String> render(MethodBodyPlan plan);
}
