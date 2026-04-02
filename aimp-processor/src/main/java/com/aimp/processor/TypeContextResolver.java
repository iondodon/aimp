package com.aimp.processor;

import java.util.List;
import java.util.Set;

interface TypeContextResolver {
    TypeContextResolution resolve(List<String> requestedTypeNames, Set<String> alreadyIncludedTypeNames, int roundNumber);
}
