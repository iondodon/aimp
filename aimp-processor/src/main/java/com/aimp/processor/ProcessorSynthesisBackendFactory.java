package com.aimp.processor;

import com.aimp.core.MethodBodySynthesizer;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

final class ProcessorSynthesisBackendFactory {
    static final String OPTION_SYNTHESIS_MODEL = "aimp.synthesis.model";
    static final String OPTION_SYNTHESIS_TIMEOUT_MILLIS = "aimp.synthesis.timeoutMillis";
    static final String OPTION_SYNTHESIS_API_KEY = "aimp.synthesis.apiKey";
    static final String OPTION_OPENAI_BASE_URL = "aimp.synthesis.openai.baseUrl";

    private static final String DEFAULT_OPENAI_BASE_URL = "https://api.openai.com";
    private static final String DEFAULT_OPENAI_MODEL = "gpt-5";
    private static final String DEFAULT_OPENAI_API_KEY_ENV = "OPENAI_API_KEY";

    private final Function<String, String> envLookup;

    ProcessorSynthesisBackendFactory() {
        this(System::getenv);
    }

    ProcessorSynthesisBackendFactory(Function<String, String> envLookup) {
        this.envLookup = Objects.requireNonNull(envLookup, "envLookup");
    }

    MethodBodySynthesizer create(Map<String, String> options) {
        return create(options, ignored -> {
        });
    }

    MethodBodySynthesizer create(Map<String, String> options, Consumer<String> logger) {
        String model = trimToNull(options.get(OPTION_SYNTHESIS_MODEL));
        if (model == null) {
            model = DEFAULT_OPENAI_MODEL;
        }

        Duration timeout = timeout(options);
        String apiKey = trimToNull(options.get(OPTION_SYNTHESIS_API_KEY));
        if (apiKey == null) {
            apiKey = trimToNull(envLookup.apply(DEFAULT_OPENAI_API_KEY_ENV));
            if (apiKey == null) {
                throw new MethodBodySynthesisException(
                    "AIMP requires the " + DEFAULT_OPENAI_API_KEY_ENV + " environment variable or -A"
                        + OPTION_SYNTHESIS_API_KEY
                        + "=... for compile-time OpenAI synthesis."
                );
            }
        }

        String baseUrl = trimToNull(options.get(OPTION_OPENAI_BASE_URL));
        if (baseUrl == null) {
            baseUrl = DEFAULT_OPENAI_BASE_URL;
        }

        return new OpenAiMethodBodySynthesizer(OpenAiEndpoints.responsesEndpoint(baseUrl), model, timeout, apiKey, logger);
    }

    private Duration timeout(Map<String, String> options) {
        String timeoutMillisValue = trimToNull(options.get(OPTION_SYNTHESIS_TIMEOUT_MILLIS));
        if (timeoutMillisValue == null) {
            return Duration.ofSeconds(30);
        }
        try {
            return Duration.ofMillis(Long.parseLong(timeoutMillisValue));
        } catch (NumberFormatException exception) {
            throw new MethodBodySynthesisException(
                "Invalid integer value for -" + OPTION_SYNTHESIS_TIMEOUT_MILLIS + ": " + timeoutMillisValue,
                exception
            );
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
