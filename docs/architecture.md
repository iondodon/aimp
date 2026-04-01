# Architecture

## Pipeline

1. `aimp-processor` discovers methods annotated with `@AIImplemented`.
2. The processor validates supported usage.
3. Source elements are mapped into the `aimp-model` contract model.
4. The processor invokes OpenAI during compilation, sending a fixed JSON request document that contains the handwritten contract source file, current round metadata, any already included type-context source files, optional request-resolution feedback from the previous round, and the required response contract.
5. If OpenAI needs more source context, it returns a fixed JSON response with `responseType = request_context_types`, and the processor resolves those type requests against a bounded reachable-type graph before calling OpenAI again with the full defining source files for the fulfillable types.
6. OpenAI ultimately returns either `responseType = generated_class` with the complete `*_AIGenerated` Java source file in `generatedClassSource`, or `responseType = insufficient_context` with a caller-facing request for more business context.
7. The processor turns caller-facing insufficient-context responses into compiler errors that surface the model's requested missing context; otherwise it validates the returned source shape and writes it through `Filer`.

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
- AIMP starts with contract-only context, then lets the model request additional source-defined types round by round.
- The maximum number of synthesis rounds is configurable with `-Aaimp.synthesis.maxRounds=...` and defaults to `6`.
- Every model round uses the same fixed JSON request/response contract.
- If OpenAI cannot infer a safe implementation even after those bounded follow-up rounds, the processor fails compilation instead of generating a fallback `UnsupportedOperationException`.
- The processor strips code fences, validates package/class/inheritance shape, and writes the result.
- Framework annotations are no longer propagated through `aimp.yml`; the LLM decides which contract annotations to copy into the generated implementation.
- The built-in processor backend calls OpenAI's Responses API directly.
