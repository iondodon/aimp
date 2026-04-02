package com.aimp.processor;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.aimp.annotations.AIContract;
import com.aimp.annotations.AIImplemented;
import com.aimp.core.GeneratedTypeNaming;
import com.aimp.model.AnnotationUsage;
import com.aimp.model.ConstructorModel;
import com.aimp.model.ContractKind;
import com.aimp.model.ContractModel;
import com.aimp.model.GeneratedElementKind;
import com.aimp.model.MethodModel;
import com.aimp.model.ParameterModel;
import com.aimp.model.TypeParameterModel;
import com.aimp.model.Visibility;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.SimpleTypeVisitor14;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Annotation processor that discovers {@link AIImplemented} contract methods and
 * generates concrete implementations in generated sources.
 */
public final class AimpProcessor extends AbstractProcessor {
    private static final String OPTION_PROJECT_DIR = "aimp.projectDir";

    private final ProcessorSynthesisBackendFactory synthesisBackendFactory = new ProcessorSynthesisBackendFactory();
    private final Set<String> generatedTypeNames = new TreeSet<>();
    private final Map<Path, SqliteGeneratedImplementationStore> implementationStores = new LinkedHashMap<>();

    private GeneratedClassSynthesizer generatedClassSynthesizer;

    private Messager messager;
    private Trees trees;

    /**
     * Creates a processor instance for javac service loading.
     */
    public AimpProcessor() {
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.messager = processingEnv.getMessager();
        try {
            this.trees = Trees.instance(processingEnv);
        } catch (IllegalArgumentException ignored) {
            this.trees = null;
        }
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Set.of(
            OPTION_PROJECT_DIR,
            ProcessorSynthesisBackendFactory.OPTION_SYNTHESIS_MODEL,
            ProcessorSynthesisBackendFactory.OPTION_SYNTHESIS_TIMEOUT_MILLIS,
            ProcessorSynthesisBackendFactory.OPTION_SYNTHESIS_API_KEY,
            ProcessorSynthesisBackendFactory.OPTION_OPENAI_BASE_URL,
            ProcessorSynthesisBackendFactory.OPTION_SYNTHESIS_MAX_ROUNDS
        );
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(AIImplemented.class.getCanonicalName(), AIContract.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return true;
        }

        Map<TypeElement, List<ExecutableElement>> methodsByContract = new LinkedHashMap<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(AIImplemented.class)) {
            if (!(element instanceof ExecutableElement method)) {
                error(element, "@AIImplemented can only be applied to methods.");
                continue;
            }
            if (!(method.getEnclosingElement() instanceof TypeElement contractType)) {
                error(method, "@AIImplemented method must be declared inside an interface or abstract class.");
                continue;
            }
            methodsByContract.computeIfAbsent(contractType, ignored -> new ArrayList<>()).add(method);
        }

        if (methodsByContract.isEmpty()) {
            return true;
        }

        for (Map.Entry<TypeElement, List<ExecutableElement>> entry : methodsByContract.entrySet()) {
            ContractModel contract = buildContract(entry.getKey(), entry.getValue());
            if (contract == null) {
                continue;
            }

            String generatedQualifiedName = GeneratedTypeNaming.generatedQualifiedName(contract.packageName(), contract.simpleName());
            String generatedSource;

            SqliteGeneratedImplementationStore implementationStore;
            try {
                implementationStore = implementationStore(entry.getKey());
            } catch (GeneratedImplementationStoreException exception) {
                error(entry.getKey(), "Failed to open the AIMP implementation store: " + exception.getMessage());
                continue;
            }

            StoredGeneratedImplementation storedImplementation;
            try {
                storedImplementation = implementationStore.find(contract.qualifiedName(), contract.version()).orElse(null);
            } catch (GeneratedImplementationStoreException exception) {
                error(entry.getKey(), "Failed to read the AIMP implementation store: " + exception.getMessage());
                continue;
            }

            if (storedImplementation != null) {
                if (!generatedQualifiedName.equals(storedImplementation.generatedQualifiedName())) {
                    error(
                        entry.getKey(),
                        "Stored generated implementation for "
                            + contract.qualifiedName()
                            + " version "
                            + contract.version()
                            + " targets "
                            + storedImplementation.generatedQualifiedName()
                            + " instead of "
                            + generatedQualifiedName
                            + "."
                    );
                    continue;
                }
                generatedSource = storedImplementation.generatedSource();
                note(
                    "AIMP reused persisted implementation for contract "
                        + contract.qualifiedName()
                        + " version "
                        + contract.version()
                );
            } else {
                if (!ensureGeneratedClassSynthesizer()) {
                    return true;
                }
                try {
                    generatedSource = generatedClassSynthesizer.synthesize(contract, createTypeContextResolver(entry.getKey()));
                } catch (MethodBodySynthesisException exception) {
                    error(entry.getKey(), "Failed to synthesize generated class: " + exception.getMessage());
                    continue;
                }

                try {
                    implementationStore.save(new StoredGeneratedImplementation(
                        contract.qualifiedName(),
                        contract.version(),
                        generatedQualifiedName,
                        generatedSource
                    ));
                } catch (GeneratedImplementationStoreException exception) {
                    error(entry.getKey(), "Failed to persist the generated implementation: " + exception.getMessage());
                    continue;
                }

                note(
                    "AIMP stored generated implementation for contract "
                        + contract.qualifiedName()
                        + " version "
                        + contract.version()
                );
            }

            if (generatedTypeNames.contains(generatedQualifiedName)) {
                continue;
            }

            try {
                JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(generatedQualifiedName, entry.getKey());
                try (Writer writer = sourceFile.openWriter()) {
                    writer.write(generatedSource);
                }
                generatedTypeNames.add(generatedQualifiedName);
            } catch (FilerException ignored) {
                generatedTypeNames.add(generatedQualifiedName);
            } catch (IOException exception) {
                error(entry.getKey(), "Failed to write generated source " + generatedQualifiedName + ": " + exception.getMessage());
            }
        }

        return true;
    }

    private SqliteGeneratedImplementationStore implementationStore(TypeElement contractType) {
        Path projectDirectory = resolveProjectDirectory(contractType);
        return implementationStores.computeIfAbsent(
            projectDirectory,
            path -> new SqliteGeneratedImplementationStore(path.resolve(".aimp").resolve("aimp.db"))
        );
    }

    private Path resolveProjectDirectory(TypeElement contractType) {
        String projectDirectoryOption = trimToNull(processingEnv.getOptions().get(OPTION_PROJECT_DIR));
        if (projectDirectoryOption != null) {
            return Path.of(projectDirectoryOption).toAbsolutePath().normalize();
        }

        if (trees != null) {
            TreePath path = trees.getPath(contractType);
            if (path != null && path.getCompilationUnit() != null && path.getCompilationUnit().getSourceFile() != null) {
                URI sourceFileUri = path.getCompilationUnit().getSourceFile().toUri();
                if ("file".equalsIgnoreCase(sourceFileUri.getScheme())) {
                    Path sourcePath = Paths.get(sourceFileUri).toAbsolutePath().normalize();
                    Path inferredProjectDirectory = inferProjectDirectory(sourcePath);
                    if (inferredProjectDirectory != null) {
                        return inferredProjectDirectory;
                    }
                }
            }
        }

        return Paths.get("").toAbsolutePath().normalize();
    }

    private Path inferProjectDirectory(Path sourcePath) {
        for (Path current = sourcePath.getParent(); current != null; current = current.getParent()) {
            if (current.getFileName() != null && "src".equals(current.getFileName().toString())) {
                Path parent = current.getParent();
                if (parent != null) {
                    return parent;
                }
            }
        }
        return sourcePath.getParent();
    }

    private boolean ensureGeneratedClassSynthesizer() {
        if (generatedClassSynthesizer != null) {
            return true;
        }

        try {
            generatedClassSynthesizer = synthesisBackendFactory.create(processingEnv.getOptions(), this::note);
            return true;
        } catch (MethodBodySynthesisException exception) {
            error("Failed to configure compile-time synthesis: " + exception.getMessage());
            return false;
        }
    }

    private void note(String message) {
        messager.printMessage(Diagnostic.Kind.NOTE, message);
    }

    private TypeContextResolver createTypeContextResolver(TypeElement contractType) {
        return new JavacTypeContextResolver(contractType);
    }

    private ContractModel buildContract(TypeElement contractType, List<ExecutableElement> methods) {
        ContractKind contractKind = resolveContractKind(contractType);
        if (contractKind == null) {
            return null;
        }

        if (contractType.getNestingKind() != NestingKind.TOP_LEVEL) {
            error(contractType, "Nested contracts are not supported in AIMP v1.");
            return null;
        }

        if (contractType.getModifiers().contains(Modifier.FINAL)) {
            error(contractType, "@AIImplemented cannot be used inside a final type.");
            return null;
        }

        String packageName = processingEnv.getElementUtils().getPackageOf(contractType).getQualifiedName().toString();
        String generatedQualifiedName = GeneratedTypeNaming.generatedQualifiedName(packageName, contractType.getSimpleName().toString());
        TypeElement existingGeneratedType = processingEnv.getElementUtils().getTypeElement(generatedQualifiedName);
        if (existingGeneratedType != null && !generatedTypeNames.contains(generatedQualifiedName)) {
            error(contractType, "Generated class name conflicts with an existing type: " + generatedQualifiedName);
            return null;
        }

        List<MethodModel> methodModels = new ArrayList<>();
        boolean valid = true;
        for (ExecutableElement method : methods) {
            MethodModel model = buildMethod(contractType, contractKind, method);
            if (model == null) {
                valid = false;
                continue;
            }
            methodModels.add(model);
        }

        if (!valid || methodModels.isEmpty()) {
            return null;
        }

        List<ConstructorModel> constructors = contractKind == ContractKind.ABSTRACT_CLASS
            ? buildConstructors(contractType)
            : List.of();
        if (constructors == null) {
            return null;
        }

        String contractVersion = contractVersion(contractType);
        if (contractVersion == null) {
            return null;
        }

        return new ContractModel(
            packageName,
            contractType.getSimpleName().toString(),
            contractType.getQualifiedName().toString(),
            contractVersion,
            contractKind,
            visibilityOf(contractType),
            extractContractSourceSnippet(contractType),
            toTypeParameters(contractType),
            extractAnnotations(contractType.getAnnotationMirrors()),
            constructors,
            methodModels
        );
    }

    private String contractVersion(TypeElement contractType) {
        AIContract contractAnnotation = contractType.getAnnotation(AIContract.class);
        if (contractAnnotation == null) {
            return "1";
        }

        String version = trimToNull(contractAnnotation.version());
        if (version == null) {
            error(contractType, "@AIContract version must not be blank.");
            return null;
        }
        return version;
    }

    private ContractKind resolveContractKind(TypeElement contractType) {
        if (contractType.getKind() == ElementKind.INTERFACE) {
            return ContractKind.INTERFACE;
        }
        if (contractType.getKind() == ElementKind.CLASS && contractType.getModifiers().contains(Modifier.ABSTRACT)) {
            return ContractKind.ABSTRACT_CLASS;
        }
        error(contractType, "@AIImplemented methods must be declared inside an interface or abstract class.");
        return null;
    }

    private List<ConstructorModel> buildConstructors(TypeElement contractType) {
        List<ExecutableElement> constructors = ElementFilter.constructorsIn(contractType.getEnclosedElements());
        if (constructors.isEmpty()) {
            return List.of();
        }

        List<ConstructorModel> collected = new ArrayList<>();
        boolean valid = true;

        for (ExecutableElement constructor : constructors) {
            if (constructor.getModifiers().contains(Modifier.PRIVATE)) {
                continue;
            }
            if (!constructor.getTypeParameters().isEmpty()) {
                error(constructor, "Generic constructors are not supported in AIMP v1.");
                valid = false;
                continue;
            }
            collected.add(new ConstructorModel(
                visibilityOf(constructor),
                toParameters(constructor),
                constructor.getThrownTypes().stream().map(Object::toString).toList()
            ));
        }

        if (!valid) {
            return null;
        }

        if (collected.isEmpty()) {
            error(contractType, "Abstract class must declare at least one accessible non-private constructor.");
            return null;
        }

        return collected;
    }

    private MethodModel buildMethod(TypeElement contractType, ContractKind contractKind, ExecutableElement method) {
        AIImplemented annotation = method.getAnnotation(AIImplemented.class);
        String description = annotation == null ? "" : annotation.value().trim();
        if (description.isBlank()) {
            error(method, "@AIImplemented value must not be blank.");
            return null;
        }

        Set<Modifier> modifiers = method.getModifiers();
        if (modifiers.contains(Modifier.STATIC)) {
            error(method, "@AIImplemented does not support static methods.");
            return null;
        }
        if (modifiers.contains(Modifier.PRIVATE)) {
            error(method, "@AIImplemented does not support private methods.");
            return null;
        }
        if (modifiers.contains(Modifier.DEFAULT)) {
            error(method, "@AIImplemented does not support default interface methods in v1.");
            return null;
        }

        if (contractKind == ContractKind.ABSTRACT_CLASS && !modifiers.contains(Modifier.ABSTRACT)) {
            error(method, "@AIImplemented does not support concrete methods.");
            return null;
        }

        Visibility visibility = contractKind == ContractKind.INTERFACE ? Visibility.PUBLIC : visibilityOf(method);
        return new MethodModel(
            method.getSimpleName().toString(),
            renderType(method.getReturnType()),
            visibility,
            toTypeParameters(method),
            toParameters(method),
            method.getThrownTypes().stream().map(this::renderType).toList(),
            extractAnnotations(method.getAnnotationMirrors()),
            description
        );
    }

    private List<TypeParameterModel> toTypeParameters(Parameterizable parameterizable) {
        return parameterizable.getTypeParameters().stream()
            .map(typeParameter -> new TypeParameterModel(
                typeParameter.getSimpleName().toString(),
                typeParameter.toString()
            ))
            .toList();
    }

    private List<ParameterModel> toParameters(ExecutableElement executable) {
        List<? extends VariableElement> parameters = executable.getParameters();
        List<ParameterModel> result = new ArrayList<>(parameters.size());
        for (int index = 0; index < parameters.size(); index++) {
            VariableElement parameter = parameters.get(index);
            boolean varArgs = executable.isVarArgs() && index == parameters.size() - 1;
            result.add(new ParameterModel(
                parameter.getSimpleName().toString(),
                renderType(parameter.asType()),
                varArgs,
                extractAnnotations(parameter.getAnnotationMirrors())
            ));
        }
        return result;
    }

    private boolean isAccessibleFromGeneratedType(
        Element member,
        boolean declaringContract,
        String contractPackage,
        String declaringPackage
    ) {
        Set<Modifier> modifiers = member.getModifiers();
        if (modifiers.contains(Modifier.PRIVATE)) {
            return false;
        }
        if (declaringContract) {
            return true;
        }
        if (modifiers.contains(Modifier.PUBLIC) || modifiers.contains(Modifier.PROTECTED)) {
            return true;
        }
        return contractPackage.equals(declaringPackage);
    }

    private TypeElement superclassOf(TypeElement type) {
        TypeMirror superclass = type.getSuperclass();
        if (superclass == null || superclass.getKind() == TypeKind.NONE) {
            return null;
        }
        if (superclass instanceof javax.lang.model.type.DeclaredType declaredType
            && declaredType.asElement() instanceof TypeElement typeElement) {
            return typeElement;
        }
        return null;
    }

    private String renderType(TypeMirror typeMirror) {
        return typeMirror.accept(new AnnotationFreeTypeRenderer(), null);
    }

    private static final class AnnotationFreeTypeRenderer extends SimpleTypeVisitor14<String, Void> {
        @Override
        protected String defaultAction(TypeMirror typeMirror, Void unused) {
            return stripTypeUseAnnotations(typeMirror.toString());
        }

        @Override
        public String visitPrimitive(PrimitiveType type, Void unused) {
            return type.toString();
        }

        @Override
        public String visitNoType(NoType type, Void unused) {
            return type.getKind() == TypeKind.VOID ? "void" : type.toString();
        }

        @Override
        public String visitNull(NullType type, Void unused) {
            return type.toString();
        }

        @Override
        public String visitArray(ArrayType type, Void unused) {
            return visit(type.getComponentType()) + "[]";
        }

        @Override
        public String visitDeclared(DeclaredType type, Void unused) {
            StringBuilder builder = new StringBuilder(stripTypeUseAnnotations(type.asElement().toString()));
            if (!type.getTypeArguments().isEmpty()) {
                builder.append('<');
                for (int index = 0; index < type.getTypeArguments().size(); index++) {
                    if (index > 0) {
                        builder.append(", ");
                    }
                    builder.append(visit(type.getTypeArguments().get(index)));
                }
                builder.append('>');
            }
            return builder.toString();
        }

        @Override
        public String visitTypeVariable(TypeVariable type, Void unused) {
            return stripTypeUseAnnotations(type.asElement().toString());
        }

        @Override
        public String visitWildcard(WildcardType type, Void unused) {
            if (type.getExtendsBound() != null) {
                return "? extends " + visit(type.getExtendsBound());
            }
            if (type.getSuperBound() != null) {
                return "? super " + visit(type.getSuperBound());
            }
            return "?";
        }

        @Override
        public String visitIntersection(IntersectionType type, Void unused) {
            return type.getBounds().stream().map(this::visit).reduce((left, right) -> left + " & " + right).orElse("");
        }

        @Override
        public String visitUnion(UnionType type, Void unused) {
            return type.getAlternatives().stream().map(this::visit).reduce((left, right) -> left + " | " + right).orElse("");
        }

        @Override
        public String visitError(ErrorType type, Void unused) {
            return defaultAction(type, unused);
        }

        private static String stripTypeUseAnnotations(String rendered) {
            StringBuilder builder = new StringBuilder(rendered.length());
            int index = 0;
            while (index < rendered.length()) {
                char current = rendered.charAt(index);
                if (current == '@') {
                    index = skipAnnotation(rendered, index + 1);
                    while (index < rendered.length() && Character.isWhitespace(rendered.charAt(index))) {
                        index++;
                    }
                    continue;
                }
                builder.append(current);
                index++;
            }
            return builder.toString().replaceAll("\\s+", " ").trim();
        }

        private static int skipAnnotation(String rendered, int index) {
            while (index < rendered.length()) {
                char current = rendered.charAt(index);
                if (Character.isWhitespace(current) || current == '<' || current == '>' || current == ',' || current == '[' || current == ']') {
                    return index;
                }
                if (current == '(') {
                    int depth = 1;
                    index++;
                    while (index < rendered.length() && depth > 0) {
                        char nested = rendered.charAt(index);
                        if (nested == '(') {
                            depth++;
                        } else if (nested == ')') {
                            depth--;
                        }
                        index++;
                    }
                    return index;
                }
                index++;
            }
            return index;
        }
    }

    private List<AnnotationUsage> extractAnnotations(List<? extends AnnotationMirror> mirrors) {
        return mirrors.stream()
            .map(this::toAnnotationUsage)
            .filter(annotation -> annotation != null)
            .sorted(Comparator.comparing(AnnotationUsage::typeName))
            .toList();
    }

    private AnnotationUsage toAnnotationUsage(AnnotationMirror mirror) {
        TypeElement annotationType = (TypeElement) mirror.getAnnotationType().asElement();
        String annotationName = annotationType.getQualifiedName().toString();
        if (annotationName.equals(AIImplemented.class.getCanonicalName())
            || annotationName.equals(AIContract.class.getCanonicalName())) {
            return null;
        }
        return new AnnotationUsage(annotationName, renderAnnotation(mirror, annotationType), resolveTargets(annotationType));
    }

    private String renderAnnotation(AnnotationMirror mirror, TypeElement annotationType) {
        Map<? extends ExecutableElement, ? extends AnnotationValue> values =
            processingEnv.getElementUtils().getElementValuesWithDefaults(mirror);
        if (values.isEmpty()) {
            return "@" + annotationType.getQualifiedName();
        }

        var entries = values.entrySet().stream()
            .sorted(Comparator.comparing(entry -> entry.getKey().getSimpleName().toString()))
            .toList();

        if (entries.size() == 1 && entries.getFirst().getKey().getSimpleName().contentEquals("value")) {
            return "@" + annotationType.getQualifiedName() + "(" + entries.getFirst().getValue() + ")";
        }

        String renderedValues = entries.stream()
            .map(entry -> entry.getKey().getSimpleName() + " = " + entry.getValue())
            .reduce((left, right) -> left + ", " + right)
            .orElse("");
        return "@" + annotationType.getQualifiedName() + "(" + renderedValues + ")";
    }

    private Set<GeneratedElementKind> resolveTargets(TypeElement annotationType) {
        AnnotationMirror targetMirror = annotationType.getAnnotationMirrors().stream()
            .filter(mirror -> mirror.getAnnotationType().toString().equals("java.lang.annotation.Target"))
            .findFirst()
            .orElse(null);

        if (targetMirror == null) {
            return EnumSet.of(GeneratedElementKind.TYPE, GeneratedElementKind.METHOD, GeneratedElementKind.PARAMETER);
        }

        Set<GeneratedElementKind> targets = EnumSet.noneOf(GeneratedElementKind.class);
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : targetMirror.getElementValues().entrySet()) {
            if (!entry.getKey().getSimpleName().contentEquals("value")) {
                continue;
            }
            @SuppressWarnings("unchecked")
            List<? extends AnnotationValue> values = (List<? extends AnnotationValue>) entry.getValue().getValue();
            for (AnnotationValue value : values) {
                String elementType = value.getValue().toString();
                switch (elementType) {
                    case "TYPE" -> targets.add(GeneratedElementKind.TYPE);
                    case "METHOD" -> targets.add(GeneratedElementKind.METHOD);
                    case "PARAMETER" -> targets.add(GeneratedElementKind.PARAMETER);
                    default -> {
                    }
                }
            }
        }
        return targets;
    }

    private Visibility visibilityOf(Element element) {
        Set<Modifier> modifiers = element.getModifiers();
        if (modifiers.contains(Modifier.PUBLIC)) {
            return Visibility.PUBLIC;
        }
        if (modifiers.contains(Modifier.PROTECTED)) {
            return Visibility.PROTECTED;
        }
        return Visibility.PACKAGE_PRIVATE;
    }

    private String extractContractSourceSnippet(TypeElement contractType) {
        return extractCompilationUnitSource(contractType);
    }

    private String extractTypeSourceSnippet(TypeElement typeElement) {
        if (trees == null) {
            return "";
        }

        TreePath path = trees.getPath(typeElement);
        if (path == null) {
            return "";
        }

        CompilationUnitTree compilationUnit = path.getCompilationUnit();
        if (compilationUnit == null || compilationUnit.getSourceFile() == null) {
            return "";
        }

        try (Reader reader = compilationUnit.getSourceFile().openReader(true)) {
            StringWriter writer = new StringWriter();
            reader.transferTo(writer);
            return writer.toString().trim();
        } catch (IOException ignored) {
            return "";
        }
    }

    private String extractCompilationUnitSource(TypeElement typeElement) {
        return extractTypeSourceSnippet(typeElement);
    }

    private final class JavacTypeContextResolver implements TypeContextResolver {
        private final TypeElement contractType;
        private final String contractPackage;
        private final String contractQualifiedName;
        private final Map<String, SourceAvailableType> cache = new java.util.LinkedHashMap<>();

        private JavacTypeContextResolver(TypeElement contractType) {
            this.contractType = contractType;
            this.contractPackage = processingEnv.getElementUtils().getPackageOf(contractType).getQualifiedName().toString();
            this.contractQualifiedName = contractType.getQualifiedName().toString();
        }

        @Override
        public TypeContextResolution resolve(List<String> requestedTypeNames, Set<String> alreadyIncludedTypeNames, int roundNumber) {
            List<com.aimp.model.ReferencedTypeModel> fulfilledTypes = new ArrayList<>();
            List<RejectedContextTypeRequest> rejectedTypeRequests = new ArrayList<>();
            java.util.LinkedHashSet<String> deduplicatedRequests = new java.util.LinkedHashSet<>(requestedTypeNames);

            for (String requestedTypeName : deduplicatedRequests) {
                String typeName = requestedTypeName == null ? "" : requestedTypeName.trim();
                if (typeName.isEmpty()) {
                    rejectedTypeRequests.add(new RejectedContextTypeRequest(
                        requestedTypeName,
                        "AIMP could not supply this request because it was blank."
                    ));
                    continue;
                }
                if (!looksLikeFullyQualifiedName(typeName)) {
                    rejectedTypeRequests.add(new RejectedContextTypeRequest(
                        typeName,
                        "AIMP could not supply this request because it is not a concrete fully qualified Java type name."
                    ));
                    continue;
                }
                if (typeName.equals(contractQualifiedName)) {
                    rejectedTypeRequests.add(new RejectedContextTypeRequest(
                        typeName,
                        "AIMP already provides the contract source in contractSource."
                    ));
                    continue;
                }
                if (alreadyIncludedTypeNames.contains(typeName)) {
                    rejectedTypeRequests.add(new RejectedContextTypeRequest(
                        typeName,
                        "AIMP already included this type in an earlier round."
                    ));
                    continue;
                }

                SourceAvailableType sourceAvailableType = resolveSourceAvailableType(typeName);
                if (sourceAvailableType == null) {
                    rejectedTypeRequests.add(new RejectedContextTypeRequest(
                        typeName,
                        "AIMP could not supply this type because its source is unavailable or the type cannot be resolved."
                    ));
                    continue;
                }
                if (!isTypeAccessibleFromGeneratedType(sourceAvailableType.typeElement())) {
                    rejectedTypeRequests.add(new RejectedContextTypeRequest(
                        typeName,
                        "AIMP could not supply this type because the generated implementation cannot access it safely."
                    ));
                    continue;
                }

                fulfilledTypes.add(new com.aimp.model.ReferencedTypeModel(
                    sourceAvailableType.qualifiedName(),
                    sourceAvailableType.sourceSnippet(),
                    roundNumber
                ));
            }

            return new TypeContextResolution(fulfilledTypes, rejectedTypeRequests);
        }

        private SourceAvailableType resolveSourceAvailableType(String qualifiedName) {
            if (cache.containsKey(qualifiedName)) {
                return cache.get(qualifiedName);
            }

            TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(qualifiedName);
            if (typeElement == null) {
                cache.put(qualifiedName, null);
                return null;
            }

            String sourceSnippet = extractCompilationUnitSource(typeElement);
            if (sourceSnippet.isBlank()) {
                cache.put(qualifiedName, null);
                return null;
            }

            SourceAvailableType resolved = new SourceAvailableType(typeElement, qualifiedName, sourceSnippet);
            cache.put(qualifiedName, resolved);
            return resolved;
        }

        private boolean isTypeAccessibleFromGeneratedType(TypeElement typeElement) {
            Element current = typeElement;
            while (current instanceof TypeElement currentType) {
                Element enclosingElement = currentType.getEnclosingElement();
                String declaringPackage = processingEnv.getElementUtils().getPackageOf(currentType).getQualifiedName().toString();

                if (!isAccessibleFromGeneratedType(
                    currentType,
                    isDeclaredInsideType(currentType, contractType),
                    contractPackage,
                    declaringPackage
                )) {
                    return false;
                }

                if (!(enclosingElement instanceof TypeElement enclosingType)) {
                    return true;
                }
                current = enclosingType;
            }
            return false;
        }

        private boolean isDeclaredInsideType(TypeElement currentType, TypeElement rootType) {
            Element current = currentType;
            while (current instanceof TypeElement typeElement) {
                if (typeElement.equals(rootType)) {
                    return true;
                }
                current = typeElement.getEnclosingElement();
            }
            return false;
        }

        private boolean looksLikeFullyQualifiedName(String typeName) {
            String[] parts = typeName.split("\\.");
            if (parts.length < 2) {
                return false;
            }
            for (String part : parts) {
                if (part.isEmpty()) {
                    return false;
                }
                if (!Character.isJavaIdentifierStart(part.charAt(0))) {
                    return false;
                }
                for (int index = 1; index < part.length(); index++) {
                    if (!Character.isJavaIdentifierPart(part.charAt(index))) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private record SourceAvailableType(TypeElement typeElement, String qualifiedName, String sourceSnippet) {
    }

    private void error(Element element, String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }

    private void error(String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, message);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
