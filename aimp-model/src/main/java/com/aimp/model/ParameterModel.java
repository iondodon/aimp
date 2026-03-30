package com.aimp.model;

import java.util.List;

public record ParameterModel(String name, String type, boolean varArgs, List<AnnotationUsage> annotations) {
    public ParameterModel {
        annotations = List.copyOf(annotations);
    }
}
