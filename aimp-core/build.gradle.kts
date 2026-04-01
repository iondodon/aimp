plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":aimp-model"))

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
