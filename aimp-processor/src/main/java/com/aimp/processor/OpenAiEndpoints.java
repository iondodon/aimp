package com.aimp.processor;

import java.net.URI;

final class OpenAiEndpoints {
    private OpenAiEndpoints() {
    }

    static URI responsesEndpoint(String baseUrl) {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        return URI.create(normalizedBaseUrl).resolve("v1/responses");
    }
}
