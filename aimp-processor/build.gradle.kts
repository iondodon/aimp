plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(project(":aimp-annotations"))
    implementation(project(":aimp-core"))
    implementation(project(":aimp-model"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")

    testImplementation(project(":aimp-testkit"))
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
