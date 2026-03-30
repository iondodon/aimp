plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(project(":aimp-annotations"))
    implementation(project(":aimp-config"))
    implementation(project(":aimp-core"))
    implementation(project(":aimp-model"))

    testImplementation(project(":aimp-testkit"))
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
