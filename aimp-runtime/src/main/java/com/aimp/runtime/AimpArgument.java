package com.aimp.runtime;

import java.util.Objects;

public record AimpArgument(String name, String typeName, Object value) {
    public AimpArgument {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(typeName, "typeName");
    }
}
