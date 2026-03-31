# aimp

AIMP is a framework-agnostic Java 21 annotation processor that generates concrete implementations for user-defined contracts annotated with `@AIImplemented`.

Generated source is emitted into annotation-processor output only. Handwritten Java files are never rewritten or mutated.

The annotation processor itself synthesizes complete Java implementation classes during compilation by calling OpenAI. Generated classes are plain Java and do not require a runtime executor.

## Modules

- `aimp-annotations`: stable public annotations such as `@AIImplemented`.
- `aimp-model`: internal model for contracts, annotations, and generation plans.
- `aimp-core`: shared internal naming and planning utilities.
- `aimp-processor`: the annotation processor that discovers contracts and writes generated source files.
- `aimp-runtime`: optional support module kept for future integrations.
- `aimp-testkit`: compile-test helpers used by processor tests.

## Quick start

1. Add `com.aimp:aimp-annotations` to your compile classpath.
2. Add `com.aimp:aimp-processor` to the `annotationProcessor` configuration.
3. Annotate interface or abstract methods with `@AIImplemented("...")`.
4. Configure OpenAI-based compile-time synthesis with annotation processor options.

Example:

```java
package com.example.payment;

import com.aimp.annotations.AIImplemented;

public interface PaymentService {
    @AIImplemented("Charge a payment and return the result")
    PaymentResult charge(PaymentRequest request);
}
```

Generated shape:

```java
package com.example.payment;

public class PaymentService_AIGenerated implements PaymentService {
    @Override
    public PaymentResult charge(PaymentRequest request) {
        return new PaymentResult("approved");
    }
}
```

Compile-time backend configuration example:

```java
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Aaimp.synthesis.model=gpt-5");
}
```

The processor reads the API key from the `OPENAI_API_KEY` environment variable by default.

## Compile-Time Synthesis

- AIMP uses OpenAI's Responses API for compile-time synthesis.
- `OPENAI_API_KEY` is required unless `-Aaimp.synthesis.apiKey=...` is passed explicitly.
- `-Aaimp.synthesis.model=...` overrides the model, defaulting to `gpt-5`.
- `-Aaimp.synthesis.openai.baseUrl=...` can override the OpenAI base URL, which is mainly useful for tests or proxies.
- During compilation, AIMP emits compiler notes before and after each OpenAI synthesis call so you can see exactly which handwritten contract invoked the LLM and which generated type is being written.
- AIMP and the model communicate through a fixed JSON contract on every synthesis round. The request is a JSON document describing the contract, the current round, the available next-layer types, and the required response contract. The response is a JSON document with an explicit `responseType`.
- The processor sends contract metadata plus the full handwritten contract source in the first OpenAI round.
- If the model needs more source context, it can request a bounded next layer of source-available referenced types by fully qualified name, and AIMP will call OpenAI again with those type snippets added.
- AIMP does not let that source-type expansion go arbitrarily deep. After the bounded follow-up rounds are exhausted, the build fails and the caller must add more business context by enriching `@AIImplemented("...")` or the handwritten contract code.
- The prompt allows the model to introduce helper fields, constants, constructors, and helper methods when needed.
- Annotation copying is now owned by the generated-class prompt rather than by `aimp.yml`.
- If OpenAI sees that even the bounded type-layer expansion is not enough, it can return an explicit caller-facing missing-context request. AIMP fails compilation and surfaces that request so the caller can add the requested details and compile again.
- The returned content must always be JSON. When `responseType` is `generated_class`, the generated Java source is carried in the `generatedClassSource` field.

## Examples

- [`examples/basic-java`](examples/basic-java)
- [`examples/spring-boot-app`](examples/spring-boot-app)
- [`examples/spring-like-annotations`](examples/spring-like-annotations)

Those examples are configured to use the OpenAI provider, so compiling them requires `OPENAI_API_KEY`.

## Build

The project uses Gradle Kotlin DSL, Java toolchains targeting Java 21, JUnit 5, and the standard `annotationProcessor` configuration for processor usage.
