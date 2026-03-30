# Architecture

## Pipeline

1. `aimp-processor` discovers methods annotated with `@AIImplemented`.
2. The processor validates supported usage and loads `aimp.yml`.
3. Source elements are mapped into the `aimp-model` contract model.
4. The processor invokes OpenAI during compilation, sending method metadata together with the full handwritten contract source to obtain Java method body statements.
5. `aimp-core` converts the model and synthesized statements into a deterministic generated type plan.
6. The processor writes the generated class through `Filer`.

## Module boundaries

- `aimp-annotations` is the public annotation API and has no heavy dependencies.
- `aimp-config` parses and validates the allowlist-only propagation config.
- `aimp-model` holds the internal immutable records shared across discovery, planning, and rendering.
- `aimp-core` contains naming, propagation filtering, synthesized body planning, and Java rendering without `javac` API usage.
- `aimp-processor` is the only module that touches annotation-processing element APIs.
- `aimp-runtime` remains available for future optional integrations, but generated v1 code does not depend on it.
- `aimp-testkit` wraps `JavaCompiler` to support compile tests and golden-file checks.

## Validation rules

- Contracts must be top-level interfaces or abstract classes.
- Final contract types are rejected.
- Annotated methods must not be concrete, static, private, or default interface methods.
- Generated names must not collide with handwritten classes.
- Abstract classes must expose at least one non-private constructor if they declare constructors.

## Rendering rules

- Generated classes live in the same package as the contract.
- Interface contracts produce `implements`; abstract classes produce `extends`.
- Generated names end with `_AIGenerated`.
- Propagated annotations come only from the config allowlist.
- `@AIImplemented` is never copied.
- Synthesized method bodies are obtained during compilation and written directly into the generated class.
- The built-in processor backend calls OpenAI's Responses API directly.
