package com.aimp.processor;

import java.util.List;

record ContextRequestFeedback(
    List<String> fulfilledTypeNames,
    List<RejectedContextTypeRequest> rejectedTypeRequests
) {
    ContextRequestFeedback {
        fulfilledTypeNames = List.copyOf(fulfilledTypeNames);
        rejectedTypeRequests = List.copyOf(rejectedTypeRequests);
    }

    boolean isEmpty() {
        return fulfilledTypeNames.isEmpty() && rejectedTypeRequests.isEmpty();
    }
}
