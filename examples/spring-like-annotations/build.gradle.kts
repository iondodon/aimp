plugins {
    java
}

dependencies {
    implementation(project(":aimp-annotations"))
    annotationProcessor(project(":aimp-processor"))
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Aaimp.synthesis.model=gpt-5")
}
