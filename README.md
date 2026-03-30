# aimp

AIMP is a framework-agnostic Java 21 annotation processor that generates concrete implementations for user-defined contracts annotated with `@AIImplemented`.

Generated source is emitted into annotation-processor output only. Handwritten Java files are never rewritten or mutated.

The annotation processor itself synthesizes Java method bodies during compilation by calling a configured LLM backend. Generated classes are plain Java and do not require a runtime executor.

## Modules

- `aimp-annotations`: stable public annotations such as `@AIImplemented`.
- `aimp-config`: typed loading and validation for `aimp.yml`.
- `aimp-model`: internal model for contracts, annotations, and generation plans.
- `aimp-core`: framework-agnostic planning and Java source rendering for synthesized method bodies.
- `aimp-processor`: the annotation processor that discovers contracts and writes generated source files.
- `aimp-runtime`: optional support module kept for future integrations.
- `aimp-testkit`: compile-test helpers used by processor tests.

## Quick start

1. Add `com.aimp:aimp-annotations` to your compile classpath.
2. Add `com.aimp:aimp-processor` to the `annotationProcessor` configuration.
3. Annotate interface or abstract methods with `@AIImplemented("...")`.
4. Configure OpenAI-based compile-time synthesis with annotation processor options.
5. Optionally create `aimp.yml` in the project root to allowlist annotations for propagation.

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

## Configuration

`aimp.yml` uses an allowlist-only propagation model:

```yaml
aimp:
  propagation:
    annotations:
      - org.springframework.stereotype.Service
      - org.springframework.transaction.annotation.Transactional
      - jakarta.validation.Valid
```

Only allowlisted annotations are copied, only to the same generated element kind, and `@AIImplemented` is never propagated.

## Supported v1

- Annotated interface methods.
- Annotated abstract methods in abstract classes.
- Deterministic generated class names with `_AIGenerated`.
- Compile-time method body synthesis through OpenAI.
- Signature preservation for parameters, generics, and declared exceptions where supported.
- Config-driven annotation propagation for types, methods, and parameters.

## Unsupported v1

- Concrete, static, private, and default interface methods annotated with `@AIImplemented`.
- Nested contract types.
- Handwritten source rewriting.
- Bytecode rewriting.
- Framework-specific wiring or runtime integration.
- Automatic provider SDK integration inside AIMP itself.

## Compile-Time Synthesis

- AIMP uses OpenAI's Responses API for compile-time synthesis.
- `OPENAI_API_KEY` is required unless `-Aaimp.synthesis.apiKey=...` is passed explicitly.
- `-Aaimp.synthesis.model=...` overrides the model, defaulting to `gpt-5`.
- `-Aaimp.synthesis.openai.baseUrl=...` can override the OpenAI base URL, which is mainly useful for tests or proxies.
- During compilation, AIMP emits compiler notes before and after each OpenAI synthesis call so you can see which contract methods hit the LLM.
- The processor sends contract metadata plus the full handwritten contract source to OpenAI, then writes the returned Java statements directly into generated methods.
- The prompt explicitly forbids generic placeholder bodies like `TODO` or `Not implemented`, and tells the model which fallback `UnsupportedOperationException` message to emit if the contract still lacks enough context.
- The returned content must contain only Java statements for the method body, without code fences or surrounding method braces.

## Examples

- [`examples/basic-java`](examples/basic-java)
- [`examples/spring-boot-app`](examples/spring-boot-app)
- [`examples/spring-like-annotations`](examples/spring-like-annotations)

Those examples are configured to use the OpenAI provider, so compiling them requires `OPENAI_API_KEY`.

## Build

The project uses Gradle Kotlin DSL, Java toolchains targeting Java 21, JUnit 5, and the standard `annotationProcessor` configuration for processor usage.
