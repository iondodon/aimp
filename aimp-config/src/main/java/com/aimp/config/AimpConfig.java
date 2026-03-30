package com.aimp.config;

import java.util.LinkedHashSet;
import java.util.Set;

public record AimpConfig(PropagationConfig propagation) {
    public static final AimpConfig EMPTY = new AimpConfig(new PropagationConfig(java.util.List.of()));

    public AimpConfig {
        propagation = propagation == null ? new PropagationConfig(java.util.List.of()) : propagation;
    }

    public Set<String> annotationAllowlist() {
        return new LinkedHashSet<>(propagation.annotations());
    }
}
