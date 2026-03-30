# Architecture

## Pipeline

1. `aimp-processor` discovers methods annotated with `@AIImplemented`.
2. The processor validates supported usage.
3. Source elements are mapped into the `aimp-model` contract model.
4. The processor invokes OpenAI during compilation, sending the full handwritten contract source together with contract metadata and generated-class requirements.
5. OpenAI returns either the complete `*_AIGenerated` Java source file or an explicit insufficient-context sentinel.
6. The processor turns insufficient-context responses into compiler errors; otherwise it validates the returned source shape and writes it through `Filer`.

## Module boundaries

- `aimp-annotations` is the public annotation API and has no heavy dependencies.
- `aimp-config` remains in the repo for future structured configuration, but the default generation path no longer depends on `aimp.yml`.
- `aimp-model` holds the internal immutable records shared across discovery and prompt construction.
- `aimp-core` currently provides naming and shared internal utilities without `javac` API usage.
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
- `@AIImplemented` is never copied.
- OpenAI returns the full generated class source during compilation.
- If OpenAI cannot infer a safe implementation from the contract, the processor fails compilation instead of generating a fallback `UnsupportedOperationException`.
- The processor strips code fences, validates package/class/inheritance shape, and writes the result.
- Framework annotations are no longer propagated through `aimp.yml`; the LLM decides which contract annotations to copy into the generated implementation.
- The built-in processor backend calls OpenAI's Responses API directly.
