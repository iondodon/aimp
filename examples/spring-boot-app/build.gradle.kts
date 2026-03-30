plugins {
    java
    id("org.springframework.boot") version "3.5.11"
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.11"))
    implementation(project(":aimp-annotations"))
    annotationProcessor(project(":aimp-processor"))

    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Aaimp.synthesis.model=gpt-5")
}
