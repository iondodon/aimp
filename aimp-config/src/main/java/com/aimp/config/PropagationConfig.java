package com.aimp.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public record PropagationConfig(List<String> annotations) {
    public PropagationConfig {
        annotations = annotations == null ? List.of() : List.copyOf(new LinkedHashSet<>(new ArrayList<>(annotations)));
    }
}
