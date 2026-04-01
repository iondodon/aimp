package com.aimp.processor;

record RejectedContextTypeRequest(String qualifiedName, String reason) {
    RejectedContextTypeRequest {
        qualifiedName = qualifiedName == null ? "" : qualifiedName;
        reason = reason == null ? "" : reason;
    }
}
