import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.api.plugins.JavaPluginExtension

plugins {
    base
}

group = "com.aimp"
version = providers.gradleProperty("version").get()

allprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }
}

subprojects {
    pluginManager.withPlugin("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
            withSourcesJar()
            withJavadocJar()
        }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.release.set(21)
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }

    pluginManager.withPlugin("maven-publish") {
        extensions.configure<org.gradle.api.publish.PublishingExtension> {
            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])
                    pom {
                        name.set(project.name)
                        description.set("AIMP framework-agnostic AI implementation generator for Java contracts.")
                        url.set("https://example.com/aimp")
                        licenses {
                            license {
                                name.set("Apache License, Version 2.0")
                                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                            }
                        }
                        developers {
                            developer {
                                id.set("aimp")
                                name.set("AIMP Contributors")
                            }
                        }
                        scm {
                            connection.set("scm:git:https://example.com/aimp.git")
                            developerConnection.set("scm:git:ssh://git@example.com/aimp.git")
                            url.set("https://example.com/aimp")
                        }
                    }
                }
            }
        }
    }
}
